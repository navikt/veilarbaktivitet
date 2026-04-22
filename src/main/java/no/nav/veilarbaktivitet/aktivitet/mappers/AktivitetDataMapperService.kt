package no.nav.veilarbaktivitet.aktivitet.mappers

import lombok.RequiredArgsConstructor
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.*
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.*
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapperService.IdAndVersjon
import no.nav.veilarbaktivitet.kvp.KvpService
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@RequiredArgsConstructor
@Service
class AktivitetDataMapperService(
    private val authService: IAuthService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val userInContext: UserInContext,
    private val kvpService: KvpService,
    private val oppfolgingPeriodeService: OppfolgingsperiodeService
) {

    private fun getEndretAv(bruker: Id?): String {
        if (bruker is AktorId) return bruker.get()
        if (bruker is NavIdent) return bruker.get()
        if (bruker is Fnr) return aktorOppslagClient.hentAktorId(bruker).get()
        throw IllegalArgumentException("Bruker må være AktorId, NavIdent eller Fnr")
    }

    private fun getCurrentOppfolgingsperiode(aktorId: Person.AktorId): Oppfolgingsperiode {
        val nåværendeÅpenPeriode = oppfolgingPeriodeService.hentNåværedeÅpenPeriode(aktorId)
        if (nåværendeÅpenPeriode != null) {
            return nåværendeÅpenPeriode
        } else {
            throw IllegalArgumentException("Bruker må være AktorId, NavIdent eller Fnr")
        }
    }

    private fun getOpprettFelter(aktivitetDTO: AktivitetDTO, automatisk: Boolean, aktivitetType: AktivitetTypeData): AktivitetBareOpprettFelter {
        val aktorId = userInContext.getAktorId()
        val kontorSperreEnhet: String? = kvpService.getKontorSperreEnhet(aktorId)
            .map { it.get() }
            .orElse(null)

        return AktivitetBareOpprettFelter(
            aktorId = aktorId,
            automatiskOpprettet = automatisk,
            opprettetDato = ZonedDateTime.now(),
            malid = aktivitetDTO.malid,
            oppfolgingsperiodeId = getCurrentOppfolgingsperiode(aktorId).oppfolgingsperiodeId,
            kontorsperreEnhetId = kontorSperreEnhet,
            aktivitetType = aktivitetType,
            status = aktivitetDTO.status
        )
    }

    private fun getMuterbareFelter(aktivitetDTO: AktivitetDTO): AktivitetMuterbareFelter {
        return AktivitetMuterbareFelter(
            tittel = aktivitetDTO.tittel,
            fraDato = aktivitetDTO.fraDato,
            tilDato = aktivitetDTO.tilDato,
            beskrivelse = aktivitetDTO.beskrivelse,
            lenke = aktivitetDTO.lenke,
        )
    }

    private fun getSporingsData(): SporingsData {
        val innloggetBruker = authService.getLoggedInnUser()
        val endretAvType = if (innloggetBruker is EksternBrukerId) Innsender.BRUKER else Innsender.NAV
        val endretAv = getEndretAv(innloggetBruker)

        return SporingsData(
            endretAv,
            endretAvType,
            ZonedDateTime.now(),
        )
    }

    data class IdAndVersjon(
        val id: Long,
        val versjon: Long,
    )
    private fun requireIdAndVersjon(aktivitetDTO: AktivitetDTO): IdAndVersjon {
        val id = (aktivitetDTO.id as? String)?.let { it.ifEmpty { null } }?.toLong()
        val versjon = (aktivitetDTO.versjon as? String)?.toLong()
        if (id == null) throw IllegalStateException("Kan ikke endre aktivitet uten id")
        if (versjon == null) throw IllegalStateException("Kan ikke endre aktivitet uten versjon")
        return IdAndVersjon(id, versjon)
    }

    fun mapTilOpprettAktivitetData(aktivitetDTO: AktivitetDTO, automatisk: Boolean): AktivitetsOpprettelse {
        val aktivitetType = Helpers.Type.getData(aktivitetDTO.type)
        val opprettFelter = getOpprettFelter(aktivitetDTO, automatisk, aktivitetType)
        val muterbareFelter = getMuterbareFelter(aktivitetDTO)
        val sporing = getSporingsData()

        return when (aktivitetType) {
            AktivitetTypeData.EGENAKTIVITET -> Egenaktivitet.Opprett(
                opprettFelter, muterbareFelter, sporing, egenAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.JOBBSOEKING -> Jobbsoeking.Opprett(
                opprettFelter, muterbareFelter, sporing, stillingsoekAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.SOKEAVTALE -> Sokeavtale.Opprett(
                opprettFelter, muterbareFelter, sporing, sokeAvtaleAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.IJOBB -> Ijobb.Opprett(
                opprettFelter, muterbareFelter, sporing, iJobbAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.BEHANDLING -> Behandling.Opprett(
                opprettFelter, muterbareFelter, sporing, behandlingAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.MOTE, AktivitetTypeData.SAMTALEREFERAT -> Mote.Opprett(
                opprettFelter, muterbareFelter, sporing, moteData(aktivitetDTO)
            )
            else -> throw IllegalStateException("Unexpected value: " + aktivitetType)
        }
    }

    fun mapTilOppdaterAktivitetData(aktivitetDTO: AktivitetDTO): AktivitetsEndring {
        val id = (aktivitetDTO.id as? String)?.let { it.ifEmpty { null } }?.toLong()
        val versjon = (aktivitetDTO.versjon as? String)?.toLong()
        val aktivitetType = Helpers.Type.getData(aktivitetDTO.type)
        val sporing = getSporingsData()

        if (id == null) throw IllegalStateException("Kan ikke endre aktivitet uten id")
        if (versjon == null) throw IllegalStateException("Kan ikke endre aktivitet uten versjon")

        val muterbareFelter = getMuterbareFelter(aktivitetDTO)

        return when (aktivitetType) {
            AktivitetTypeData.EGENAKTIVITET -> Egenaktivitet.Endre(
                id, versjon,  muterbareFelter, sporing, egenAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.JOBBSOEKING -> Jobbsoeking.Endre(
                id, versjon,  muterbareFelter, sporing, stillingsoekAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.SOKEAVTALE -> Sokeavtale.Endre(
                id, versjon,  muterbareFelter, sporing, sokeAvtaleAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.IJOBB -> Ijobb.Endre(
                id, versjon,  muterbareFelter, sporing, iJobbAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.BEHANDLING -> Behandling.Endre(
                id, versjon,  muterbareFelter, sporing, behandlingAktivitetData(aktivitetDTO)
            )
            AktivitetTypeData.MOTE, AktivitetTypeData.SAMTALEREFERAT -> Mote.Endre(
                id, versjon,  muterbareFelter, sporing, moteData(aktivitetDTO)
            )
            AktivitetTypeData.STILLING_FRA_NAV -> StillingFraNav.Endre(
                id, versjon,  muterbareFelter, sporing,aktivitetDTO.getStillingFraNavData()
            )
            else -> throw IllegalStateException("Unexpected value: " + aktivitetType)
        }
    }

    fun mapTilOppdaterEtikett(aktivitetDTO: AktivitetDTO): EtikettEndring {
        val idAndVersjon = requireIdAndVersjon(aktivitetDTO)
        return EtikettEndring(
            id = idAndVersjon.id,
            versjon = idAndVersjon.versjon,
            sporingsData = getSporingsData(),
            // TODO: Handle null here??
            stillingsokEtikettData = stillingsoekAktivitetData(aktivitetDTO).stillingsoekEtikett
        )
    }

    fun mapTilOppdaterStatus(aktivitetDTO: AktivitetDTO): StatusEndring {
        val idAndVersjon = requireIdAndVersjon(aktivitetDTO)
        return StatusEndring(
            id = idAndVersjon.id,
            versjon = idAndVersjon.versjon,
            sporingsData = getSporingsData(),
            status = aktivitetDTO.status,
            avsluttetKommentar = aktivitetDTO.avsluttetKommentar,
        )
    }

    fun mapTilOppdaterReferat(aktivitetDTO: AktivitetDTO): ReferatEndring {
        val idAndVersjon = requireIdAndVersjon(aktivitetDTO)
        return ReferatEndring(
            id = idAndVersjon.id,
            versjon = idAndVersjon.versjon,
            sporingsData = getSporingsData(),
            moteData = moteData(aktivitetDTO)
        )
    }

    private fun egenAktivitetData(aktivitetDTO: AktivitetDTO): EgenAktivitetData {
        return EgenAktivitetData.builder()
            .hensikt(aktivitetDTO.hensikt)
            .oppfolging(aktivitetDTO.oppfolging)
            .build()
    }

    private fun stillingsoekAktivitetData(aktivitetDTO: AktivitetDTO): StillingsoekAktivitetData {
        return StillingsoekAktivitetData.builder()
            .stillingsoekEtikett(Helpers.Etikett.getData(aktivitetDTO.etikett))
            .kontaktPerson(aktivitetDTO.kontaktperson)
            .arbeidsgiver(aktivitetDTO.arbeidsgiver)
            .arbeidssted(aktivitetDTO.arbeidssted)
            .stillingsTittel(aktivitetDTO.stillingsTittel)
            .build()
    }

    private fun sokeAvtaleAktivitetData(aktivitetDTO: AktivitetDTO): SokeAvtaleAktivitetData {
        return SokeAvtaleAktivitetData.builder()
            .antallStillingerSokes(aktivitetDTO.antallStillingerSokes)
            .antallStillingerIUken(aktivitetDTO.antallStillingerIUken)
            .avtaleOppfolging(aktivitetDTO.avtaleOppfolging)
            .build()
    }

    private fun iJobbAktivitetData(aktivitetDTO: AktivitetDTO): IJobbAktivitetData {
        return IJobbAktivitetData.builder()
            .jobbStatusType(Helpers.JobbStatus.getData(aktivitetDTO.jobbStatus))
            .ansettelsesforhold(aktivitetDTO.ansettelsesforhold)
            .arbeidstid(aktivitetDTO.arbeidstid)
            .build()
    }

    private fun behandlingAktivitetData(aktivitetDTO: AktivitetDTO): BehandlingAktivitetData {
        return BehandlingAktivitetData.builder()
            .behandlingType(aktivitetDTO.behandlingType)
            .behandlingSted(aktivitetDTO.behandlingSted)
            .effekt(aktivitetDTO.effekt)
            .behandlingOppfolging(aktivitetDTO.behandlingOppfolging)
            .build()
    }

    private fun moteData(aktivitetDTO: AktivitetDTO): MoteData {
        return MoteData.builder()
            .adresse(aktivitetDTO.adresse)
            .forberedelser(aktivitetDTO.forberedelser)
            .kanal(aktivitetDTO.kanal)
            .referat(aktivitetDTO.referat)
            .referatPublisert(aktivitetDTO.isErReferatPublisert)
            .build()
    }
}
