package no.nav.veilarbaktivitet.aktivitet

import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsOpprettelse
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Mote
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient
import no.nav.veilarbaktivitet.eventsLogger.EventType
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.List
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

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

    fun hentAktiviteterForIdent(ident: Person): MutableList<AktivitetData?> {
        return personService.getAktorIdForPersonBruker(ident)
            .map<MutableList<AktivitetData?>>(Function { aktorId: Person.AktorId? ->
                aktivitetService.hentAktiviteterForAktorId(
                    aktorId
                )
            })
            .orElseThrow<java.lang.RuntimeException?>(Supplier { RuntimeException() })
    }

    fun hentAktiviteterUtenKontorsperre(ident: Person): MutableList<AktivitetData?> {
        return hentAktiviteterForIdent(ident)
            .stream().filter { aktivitet: AktivitetData? ->
                val kontorsperreEnhetId = aktivitet!!.getKontorsperreEnhetId()
                kontorsperreEnhetId == null || kontorsperreEnhetId.isBlank()
            }.toList()
    }

    fun hentAktivitet(id: Long): AktivitetData {
        val aktivitetData = aktivitetService.hentAktivitetMedForhaandsorientering(id)
        settLestAvBrukerHvisUlest(aktivitetData)
        return aktivitetData
    }

    var typerMedTidligereVersjonTilgjengelig: MutableList<AktivitetTypeData?> =
        List.of<AktivitetTypeData>(AktivitetTypeData.MOTE, AktivitetTypeData.SAMTALEREFERAT)

    fun hentAktivitetVersion(aktivitetId: Long, versjon: Long, erEksternBruker: Boolean): AktivitetData? {
        val aktivitet = aktivitetService.hentAktivitetVersjon(aktivitetId, versjon)
        if (!typerMedTidligereVersjonTilgjengelig.contains(aktivitet.getAktivitetType())) return null

        val moteData = aktivitet.getMoteData()
        /* Brukere skal ikke kunne se referat som ikke er publisert */
        if (moteData != null && !moteData.isReferatPublisert() && erEksternBruker) return null
        return aktivitet
    }

    fun hentAktivitetVersjoner(id: Long): MutableList<AktivitetData?> {
        return aktivitetService.hentAktivitetVersjoner(id)
            .stream()
            .filter { aktivitetData: AktivitetData? -> this.erEksternBrukerOgEndringenSkalVereSynnelig(aktivitetData!!) }
            .toList()
    }

    private fun settLestAvBrukerHvisUlest(aktivitetData: AktivitetData) {
        if (authService.erEksternBruker() && aktivitetData.getLestAvBrukerForsteGang() == null) {
            val hentetAktivitet = aktivitetService.settLestAvBrukerTidspunkt(aktivitetData.getId())
            metricService.reportAktivitetLestAvBrukerForsteGang(hentetAktivitet)
        }
    }

    private fun erEksternBrukerOgEndringenSkalVereSynnelig(aktivitetData: AktivitetData): Boolean {
        return !authService.erEksternBruker() || erSynligForEksterne(aktivitetData)
    }

    private fun referatErDeltMedBruker(nyAktivitet: AktivitetData): Boolean {
        if (nyAktivitet.getMoteData() == null) return false
        return nyAktivitet.getMoteData().isReferatPublisert()
    }

    private fun nyopprettetAktivitetKanHaReferat(nyAktivitet: AktivitetData): Boolean {
        val aktivitetstyperSomKanHaReferatNårAktivitetOpprettes: MutableList<AktivitetTypeData> =
            List.of<AktivitetTypeData?>(AktivitetTypeData.SAMTALEREFERAT)
        return aktivitetstyperSomKanHaReferatNårAktivitetOpprettes.contains(nyAktivitet.getAktivitetType())
    }

    @Transactional
    fun opprettNyAktivitet(aktivitetData: AktivitetsOpprettelse): AktivitetData? {
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
                    nyAktivitet.getAktorId(),
                    nyAktivitet.getId()
                )
            }
        }

        // dette er gjort på grunn av KVP
        return if (authService.erSystemBruker()) nyAktivitet.withKontorsperreEnhetId(null) else nyAktivitet
    }

    @Transactional
    fun oppdaterAktivitet(aktivitet: AktivitetsEndring): AktivitetData? {
        val original = hentAktivitet(aktivitet.id)
        kanEndreAktivitetGuard(original, aktivitet.versjon)
        if (original.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
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
        if (original.isAvtalt()) {
            if (original.getAktivitetType() == AktivitetTypeData.MOTE) {
                val tidOgStedEndret = erMoteTidOgStedEndret(
                    original,
                    (aktivitet as no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Mote.Endre?)!!
                )
                val detaljerEndret = erMoteDetaljerEndret(original, aktivitet)

                if (tidOgStedEndret) {
                    aktivitetService.oppdaterMoteTidStedOgKanal(original, aktivitet)
                    // Hent oppdatert aktivitet for eventuell detaljer-oppdatering
                    original = aktivitetService.hentAktivitetMedForhaandsorientering(original.getId())
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
        val fraDatoEndret = original.getFraDato() != aktivitet.muterbareFelter.fraDato
        val tilDatoEndret = original.getTilDato() != aktivitet.muterbareFelter.tilDato

        val originalMote = original.getMoteData()
        val nyMote = aktivitet.moteData

        val adresseEndret = originalMote != null && nyMote != null && (originalMote.getAdresse() != nyMote.getAdresse())
        val kanalEndret = originalMote != null && nyMote != null && (originalMote.getKanal() != nyMote.getKanal())

        return fraDatoEndret || tilDatoEndret || adresseEndret || kanalEndret
    }

    private fun erMoteDetaljerEndret(original: AktivitetData, aktivitet: Mote.Endre): Boolean {
        val tittelEndret = original.getTittel() != aktivitet.muterbareFelter.tittel
        val beskrivelseEndret = original.getBeskrivelse() != aktivitet.muterbareFelter.beskrivelse
        val motedeltakereEndret = original.getMoteData().getMotedeltakere() != aktivitet.moteData.getMotedeltakere()

        val originalMote = original.getMoteData()
        val nyMote = aktivitet.moteData

        val forberedelserEndret =
            originalMote != null && nyMote != null && (originalMote.getForberedelser() != nyMote.getForberedelser())

        return tittelEndret || beskrivelseEndret || forberedelserEndret || motedeltakereEndret
    }

    private fun oppdaterSomEksternBruker(aktivitet: AktivitetsEndring, original: AktivitetData) {
        val denneAktivitetstypenKanIkkeEndresEksternt: Boolean =
            !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(original.getAktivitetType())
        // Når behandling er avtalt må vi begrense hva som kan oppdateres til kun sluttdato for behandlingen.
        // Når behandling ikke er avtalt, skal ekstern bruker ha mulighet til å endre flere ting.
        val skalOppdatereTilDatoForAvtaltMedisinskBehandling =
            original.isAvtalt() && original.getAktivitetType() == AktivitetTypeData.BEHANDLING
        if (denneAktivitetstypenKanIkkeEndresEksternt) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Feil aktivitetstype " + original.getAktivitetType())
        }
        if (original.isAvtalt() && original.getAktivitetType() != AktivitetTypeData.BEHANDLING) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktivitet er avtalt " + original.getAktivitetType())
        }
        if (skalOppdatereTilDatoForAvtaltMedisinskBehandling) {
            aktivitetService.oppdaterAktivitetFrist(original, aktivitet)
        } else {
            aktivitetService.oppdaterAktivitet(original, aktivitet)
        }
    }

    private fun kanEndreAktivitetGuard(orginalAktivitet: AktivitetData, sisteVersjon: Long) {
        if (orginalAktivitet.getVersjon() != sisteVersjon) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        } else if (!orginalAktivitet.endringTillatt()) {
            log.warn("Kan ikke endre aktivitet {} i en ferdig status", orginalAktivitet.getId())
            throw EndringAvFerdigAktivitetException("Kan ikke endre aktivitet i en ferdig status")
        }
    }

    private fun kanEndreAktivitetEtikettGuard(orginalAktivitet: AktivitetData, aktivitet: EtikettEndring) {
        if (orginalAktivitet.getVersjon() != aktivitet.versjon) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        } else if (orginalAktivitet.getHistoriskDato() != null) {
            // Etikett skal kunne endres selv om aktivitet er fullført eller avbrutt
            log.warn("Kan ikke endre etikett på historisk aktivitet {}", orginalAktivitet.getId())
            throw EndringAvHistoriskAktivitetException("Kan ikke endre etikett på historisk aktivitet")
        }
    }

    @Transactional
    fun oppdaterStatus(aktivitet: StatusEndring): AktivitetData {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        kanEndreAktivitetGuard(originalAktivitet, aktivitet.versjon)

        if (authService.erEksternBruker() && !TYPER_SOM_KAN_ENDRES_EKSTERNT.contains(originalAktivitet.getAktivitetType())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        aktivitetService.oppdaterStatus(originalAktivitet, aktivitet)
        val nyAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(originalAktivitet.getId())
        metricService.oppdatertStatus(nyAktivitet, authService.erInternBruker())

        return nyAktivitet
    }

    @Transactional
    fun oppdaterEtikett(aktivitet: EtikettEndring): AktivitetData? {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        kanEndreAktivitetEtikettGuard(originalAktivitet, aktivitet)
        aktivitetService.oppdaterEtikett(originalAktivitet, aktivitet)
        return aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.id)
    }

    @Transactional
    fun oppdaterReferat(aktivitet: ReferatEndring): AktivitetData? {
        val originalAktivitet = hentAktivitet(aktivitet.id)
        aktivitet.kanEndreAktivitetGuard(originalAktivitet, aktivitet.versjon)

        val oppdatertAktivitet = aktivitetService.oppdaterReferat(originalAktivitet, aktivitet)

        val maybeEventType = hentEventTypeForReferat(originalAktivitet, aktivitet)
        maybeEventType.ifPresent(Consumer { eventType: EventType? ->
            bigQueryClient.logEvent(oppdatertAktivitet, eventType!!)
            sendMeldingTilOversikten(oppdatertAktivitet, eventType)
        })
        return oppdatertAktivitet
    }

    private fun sendMeldingTilOversikten(aktivitet: AktivitetData, eventType: EventType) {
        if (eventType == EventType.SAMTALEREFERAT_OPPRETTET) { // Kan kun skje for aktivitetstype "Møte"
            oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(aktivitet.getAktorId(), aktivitet.getId())
        } else if (eventType == EventType.SAMTALEREFERAT_DELT_MED_BRUKER) {
            oversiktenService.lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktivitet.getAktorId(), aktivitet.getId())
        }
    }

    private fun hentEventTypeForReferat(
        originalAktivitet: AktivitetData,
        oppdatertAktivitet: ReferatEndring
    ): Optional<EventType> {
        val forrigeReferat = Optional.ofNullable<MoteData?>(originalAktivitet.getMoteData())
            .map<String?>(Function { it: MoteData? -> it!!.getReferat() }).orElse("")
        val nesteReferat = Optional.ofNullable<MoteData>(oppdatertAktivitet.moteData)
            .map<String?>(Function { it: MoteData? -> it!!.getReferat() }).orElse("")

        val referatHarNåFåttInnhold = forrigeReferat!!.isEmpty() && !nesteReferat!!.isEmpty()
        val referatHarNåBlittDeltMedBruker =
            !originalAktivitet.getMoteData().isReferatPublisert() && oppdatertAktivitet.moteData.isReferatPublisert()
        val referatVarAlleredeDelt = originalAktivitet.getMoteData().isReferatPublisert()

        if (referatHarNåFåttInnhold && !referatHarNåBlittDeltMedBruker && !referatVarAlleredeDelt) {
            return Optional.of<EventType>(EventType.SAMTALEREFERAT_OPPRETTET)
        } else if (referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
            return Optional.of<EventType>(EventType.SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER)
        } else if (!referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
            return Optional.of<EventType>(EventType.SAMTALEREFERAT_DELT_MED_BRUKER)
        } else if (referatHarNåFåttInnhold && referatVarAlleredeDelt) {
            return Optional.of<EventType>(EventType.SAMTALEREFERAT_FIKK_INNHOLD)
        } else {
            return Optional.empty<EventType>()
        }
    }

    companion object {
        private val TYPER_SOM_KAN_ENDRES_EKSTERNT: MutableSet<AktivitetTypeData?> = HashSet<AktivitetTypeData?>(
            Arrays.asList<AktivitetTypeData?>(
                AktivitetTypeData.EGENAKTIVITET,
                AktivitetTypeData.JOBBSOEKING,
                AktivitetTypeData.SOKEAVTALE,
                AktivitetTypeData.IJOBB,
                AktivitetTypeData.BEHANDLING,
                AktivitetTypeData.STILLING_FRA_NAV
            )
        )

        private val TYPER_SOM_KAN_OPPRETTES_EKSTERNT: MutableSet<AktivitetTypeData?> = HashSet<AktivitetTypeData?>(
            Arrays.asList<AktivitetTypeData?>(
                AktivitetTypeData.BEHANDLING,
                AktivitetTypeData.EGENAKTIVITET,
                AktivitetTypeData.JOBBSOEKING,
                AktivitetTypeData.IJOBB
            )
        )

        private fun erSynligForEksterne(aktivitetData: AktivitetData): Boolean {
            return !(kanHaInterneForandringer(aktivitetData) && erReferatetEndretForDetErPublisert(aktivitetData))
        }

        private fun kanHaInterneForandringer(aktivitetData: AktivitetData): Boolean {
            return aktivitetData.getAktivitetType() == AktivitetTypeData.MOTE ||
                    aktivitetData.getAktivitetType() == AktivitetTypeData.SAMTALEREFERAT
        }

        private fun erReferatetEndretForDetErPublisert(aktivitetData: AktivitetData): Boolean {
            val referatEndret = AktivitetTransaksjonsType.REFERAT_ENDRET == aktivitetData.getTransaksjonsType() ||
                    AktivitetTransaksjonsType.REFERAT_OPPRETTET == aktivitetData.getTransaksjonsType()
            return !aktivitetData.getMoteData().isReferatPublisert() && referatEndret
        }
    }
}
