package no.nav.veilarbaktivitet.aktivitet

import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.TYPER_SOM_KAN_ENDRES_EKSTERNT
import no.nav.veilarbaktivitet.aktivitet.domain.TYPER_SOM_KAN_OPPRETTES_EKSTERNT
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsOpprettelse
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Mote
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.domain.erAktivitetsVersjonSynligForEksterne
import no.nav.veilarbaktivitet.aktivitet.domain.hentEventTypeForReferat
import no.nav.veilarbaktivitet.aktivitet.domain.nyopprettetAktivitetKanHaReferat
import no.nav.veilarbaktivitet.aktivitet.domain.referatErDeltMedBruker
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient
import no.nav.veilarbaktivitet.eventsLogger.EventType
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AktivitetAppService(
    private val authService: IAuthService,
    private val aktivitetService: AktivitetService,
    private val metricService: MetricService,
    private val personService: PersonService,
    private val bigQueryClient: BigQueryClient,
    private val oversiktenService: OversiktenService
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    fun hentAktiviteterForIdent(ident: Person): List<AktivitetData> {
        return personService.getAktorIdForPersonBruker(ident)
            .map { aktorId -> aktivitetService.hentAktiviteterForAktorId(aktorId) }
            .orElseThrow { RuntimeException() }
    }

    @TestOnly
    fun hentAktiviteterUtenKontorsperre(ident: Person): List<AktivitetData> {
        return hentAktiviteterForIdent(ident)
            .filter { aktivitet ->
                val kontorsperreEnhetId = aktivitet.kontorsperreEnhetId
                kontorsperreEnhetId == null || kontorsperreEnhetId.isBlank()
            }
    }

    fun hentAktivitet(id: Long): AktivitetData {
        val aktivitetData = aktivitetService.hentAktivitetMedForhaandsorientering(id)
        settLestAvBrukerHvisUlest(aktivitetData)
        return aktivitetData
    }

    var typerMedTidligereVersjonTilgjengelig: List<AktivitetTypeData> =
        listOf(AktivitetTypeData.MOTE, AktivitetTypeData.SAMTALEREFERAT)

    fun hentAktivitetVersion(aktivitetId: Long, versjon: Long, erEksternBruker: Boolean): AktivitetData? {
        val aktivitet = aktivitetService.hentAktivitetVersjon(aktivitetId, versjon)
        if (!typerMedTidligereVersjonTilgjengelig.contains(aktivitet.aktivitetType)) return null

        val moteData = aktivitet.moteData
        /* Brukere skal ikke kunne se referat som ikke er publisert */
        if (moteData != null && !moteData.isReferatPublisert && erEksternBruker) return null
        return aktivitet
    }

    fun hentAktivitetVersjoner(id: Long): List<AktivitetData> {
        return aktivitetService.hentAktivitetVersjoner(id)
            .filter { aktivitetData -> this.erEksternBrukerOgEndringenSkalVereSynnelig(aktivitetData) }
    }

    private fun settLestAvBrukerHvisUlest(aktivitetData: AktivitetData) {
        if (authService.erEksternBruker() && aktivitetData.lestAvBrukerForsteGang == null) {
            val hentetAktivitet = aktivitetService.settLestAvBrukerTidspunkt(aktivitetData.id)
            metricService.reportAktivitetLestAvBrukerForsteGang(hentetAktivitet)
        }
    }

    private fun erEksternBrukerOgEndringenSkalVereSynnelig(aktivitetData: AktivitetData): Boolean {
        return !authService.erEksternBruker() || erAktivitetsVersjonSynligForEksterne(aktivitetData)
    }

    @Transactional
    fun opprettNyAktivitet(aktivitetData: AktivitetsOpprettelse): AktivitetData {
        if (aktivitetData.opprettFelter.aktivitetType == AktivitetTypeData.STILLING_FRA_NAV) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        if (authService.erEksternBruker() && !TYPER_SOM_KAN_OPPRETTES_EKSTERNT.contains(aktivitetData.opprettFelter.aktivitetType)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Eksternbruker kan ikke opprette denne aktivitetstypen. Fikk: " + aktivitetData.opprettFelter.aktivitetType
            )
        }

        val nyAktivitet = aktivitetService.opprettAktivitetIDB(aktivitetData)

        if (nyopprettetAktivitetKanHaReferat(nyAktivitet)) {
            if (referatErDeltMedBruker(nyAktivitet)) {
                bigQueryClient.logEvent(nyAktivitet, EventType.SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER)
            } else {
                bigQueryClient.logEvent(nyAktivitet, EventType.SAMTALEREFERAT_OPPRETTET)
                oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(
                    nyAktivitet.aktorId,
                    nyAktivitet.id
                )
            }
        }

        // dette er gjort på grunn av KVP
        return if (authService.erSystemBruker()) nyAktivitet.withKontorsperreEnhetId(null) else nyAktivitet
    }

    @Transactional
    fun oppdaterAktivitet(aktivitet: AktivitetsEndring): AktivitetData {
        val original = hentAktivitet(aktivitet.id)
        if (original.aktivitetType != aktivitet.aktivitetType)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Forrige versjon av aktivitet var av typen ${original.aktivitetType} men innkomende versjon var ${aktivitet.aktivitetType}")
        kanEndreAktivitetGuard(original, aktivitet.versjon)
        if (original.aktivitetType == AktivitetTypeData.STILLING_FRA_NAV) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        if (authService.erInternBruker()) {
            oppdaterSomNav(aktivitet, original)
            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.id)
        } else if (authService.erEksternBruker()) {
            oppdaterSomEksternBruker(aktivitet, original)
            return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.id)
        }

        // not a valid user
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }

    private fun oppdaterSomNav(aktivitet: AktivitetsEndring, original: AktivitetData) {
        var original = original
        if (original.isAvtalt) {
            if (original.aktivitetType == AktivitetTypeData.MOTE) {
                val tidOgStedEndret = erMoteTidOgStedEndret(
                    original,
                    (aktivitet as Mote.Endre)
                )
                val detaljerEndret = erMoteDetaljerEndret(original, aktivitet)

                if (tidOgStedEndret) {
                    aktivitetService.oppdaterMoteTidStedOgKanal(original, aktivitet)
                    // Hent oppdatert aktivitet for eventuell detaljer-oppdatering
                    original = aktivitetService.hentAktivitetMedForhaandsorientering(original.id)
                }
                if (detaljerEndret) {
                    aktivitetService.oppdaterMoteDetaljer(original, aktivitet)
                }
            } else {
                aktivitetService.oppdaterAktivitetFrist(original, aktivitet)
            }
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet)
        }
    }

    private fun erMoteTidOgStedEndret(original: AktivitetData, aktivitet: Mote.Endre): Boolean {
        val fraDatoEndret = original.fraDato != aktivitet.muterbareFelter.fraDato
        val tilDatoEndret = original.tilDato != aktivitet.muterbareFelter.tilDato

        val originalMote = original.moteData
        val nyMote = aktivitet.moteData

        val adresseEndret = originalMote != null && (originalMote.adresse != nyMote.adresse)
        val kanalEndret = originalMote != null && (originalMote.kanal != nyMote.kanal)

        return fraDatoEndret || tilDatoEndret || adresseEndret || kanalEndret
    }

    private fun erMoteDetaljerEndret(original: AktivitetData, aktivitet: Mote.Endre): Boolean {
        val tittelEndret = original.tittel != aktivitet.muterbareFelter.tittel
        val beskrivelseEndret = original.beskrivelse != aktivitet.muterbareFelter.beskrivelse
        val motedeltakereEndret = original.moteData?.motedeltakere != aktivitet.moteData.motedeltakere

        val originalMote = original.moteData
        val nyMote = aktivitet.moteData

        val forberedelserEndret =
            originalMote != null && (originalMote.forberedelser != nyMote.forberedelser)

        return tittelEndret || beskrivelseEndret || forberedelserEndret || motedeltakereEndret
    }

    private fun oppdaterSomEksternBruker(aktivitet: AktivitetsEndring, original: AktivitetData) {
        val denneAktivitetstypenKanIkkeEndresEksternt: Boolean =
            !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(original.aktivitetType)
        // Når behandling er avtalt må vi begrense hva som kan oppdateres til kun sluttdato for behandlingen.
        // Når behandling ikke er avtalt, skal ekstern bruker ha mulighet til å endre flere ting.
        val skalOppdatereTilDatoForAvtaltMedisinskBehandling =
            original.isAvtalt && original.aktivitetType == AktivitetTypeData.BEHANDLING
        if (denneAktivitetstypenKanIkkeEndresEksternt) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Feil aktivitetstype " + original.aktivitetType)
        }
        if (original.isAvtalt && original.aktivitetType != AktivitetTypeData.BEHANDLING) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktivitet er avtalt " + original.aktivitetType)
        }
        if (skalOppdatereTilDatoForAvtaltMedisinskBehandling) {
            aktivitetService.oppdaterAktivitetFrist(original, aktivitet)
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet)
        }
    }

    private fun kanEndreAktivitetGuard(orginalAktivitet: AktivitetData, sisteVersjon: Long) {
        if (orginalAktivitet.versjon != sisteVersjon) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        } else if (!orginalAktivitet.endringTillatt()) {
            log.warn("Kan ikke endre aktivitet {} i en ferdig status", orginalAktivitet.getId())
            throw EndringAvFerdigAktivitetException("Kan ikke endre aktivitet i en ferdig status")
        }
    }

    private fun kanEndreAktivitetEtikettGuard(orginalAktivitet: AktivitetData, aktivitet: EtikettEndring) {
        if (orginalAktivitet.versjon != aktivitet.versjon) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        } else if (orginalAktivitet.historiskDato != null) {
            // Etikett skal kunne endres selv om aktivitet er fullført eller avbrutt
            log.warn("Kan ikke endre etikett på historisk aktivitet {}", orginalAktivitet.getId())
            throw EndringAvHistoriskAktivitetException("Kan ikke endre etikett på historisk aktivitet")
        }
    }

    @Transactional
    fun oppdaterStatus(aktivitet: StatusEndring): AktivitetData {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        kanEndreAktivitetGuard(originalAktivitet, aktivitet.versjon)

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(originalAktivitet.aktivitetType)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        aktivitetService.oppdaterStatus(originalAktivitet, aktivitet)
        val nyAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(originalAktivitet.id)
        metricService.oppdatertStatus(nyAktivitet, authService.erInternBruker())

        return nyAktivitet
    }

    @Transactional
    fun oppdaterEtikett(aktivitet: EtikettEndring): AktivitetData {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        kanEndreAktivitetEtikettGuard(originalAktivitet, aktivitet)
        aktivitetService.oppdaterEtikett(originalAktivitet, aktivitet)
        return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.id)
    }

    @Transactional
    fun oppdaterReferat(aktivitet: ReferatEndring): AktivitetData {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        aktivitet.kanEndreAktivitetGuard(originalAktivitet, aktivitet.versjon)

        val oppdatertAktivitet = aktivitetService.oppdaterReferat(originalAktivitet, aktivitet)
        val forrigeMøteData = originalAktivitet.moteData ?: throw IllegalStateException("Tidligere versjon av møte hadde null som møtedata")

        val maybeEventType = hentEventTypeForReferat(forrigeMøteData, aktivitet)
        maybeEventType?.let { eventType ->
            bigQueryClient.logEvent(oppdatertAktivitet, eventType)
            sendMeldingTilOversikten(oppdatertAktivitet, eventType)
        }
        return oppdatertAktivitet
    }

    private fun sendMeldingTilOversikten(aktivitet: AktivitetData, eventType: EventType) {
        if (eventType == EventType.SAMTALEREFERAT_OPPRETTET) { // Kan kun skje for aktivitetstype "Møte"
            oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(aktivitet.aktorId, aktivitet.id)
        } else if (eventType == EventType.SAMTALEREFERAT_DELT_MED_BRUKER) {
            oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktivitet.aktorId, aktivitet.id)
        }
    }
}
