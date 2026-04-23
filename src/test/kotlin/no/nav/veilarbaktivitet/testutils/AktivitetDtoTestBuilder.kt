package no.nav.veilarbaktivitet.testutils

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.avtalt_med_nav.Type
import no.nav.veilarbaktivitet.mock.TestData
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.apache.commons.lang3.time.DateUtils
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

object AktivitetDtoTestBuilder {
    @JvmStatic
    fun nyAktivitet(aktivitetTypeDTO: AktivitetTypeDTO?): AktivitetDTO {
        //TODO implementer denne ordenltig
        val aktivitetData = AktivitetDataTestBuilder.nyAktivitet(Helpers.Type.getData(aktivitetTypeDTO))
        val aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        aktivitetDTO.setId(null)
        aktivitetDTO.setVersjon(null)
        return aktivitetDTO
    }

    fun nyOpprettFelter(
        aktivitetType: AktivitetTypeData,
        oppfolgingsperiode: UUID = UUID.randomUUID(),
        aktorId: Person.AktorId = TestData.KJENT_AKTOR_ID,
        kontorSperreEnhet: String? = null): AktivitetBareOpprettFelter {
        return AktivitetBareOpprettFelter(
            aktorId,
            aktivitetType,
            AktivitetStatus.BRUKER_ER_INTERESSERT,
            kontorSperreEnhet,
            "2",
            nyZonedDateTime(),
            false,
            oppfolgingsperiode
        )
    }

    fun nyMuterbareFelter(): AktivitetMuterbareFelter {
        return AktivitetMuterbareFelter(
            "tittel",
            "beskrivelse",
            nyDato(),
            nyDato(),
            "lenke"
        )
    }

    fun nySporingsData(aktorId: Person.AktorId? = null): SporingsData {
        return SporingsData(
            aktorId?.get() ?: TestData.KJENT_SAKSBEHANDLER.get(),
            Innsender.NAV,
            nyZonedDateTime()
        )
    }

    fun nyAktivitet(aktivitetTypeData: AktivitetTypeData): AktivitetsOpprettelse {
        return when (aktivitetTypeData) {
            AktivitetTypeData.MOTE -> nyMoteAktivitet()
            AktivitetTypeData.IJOBB -> nyIJobbAktivitet()
            AktivitetTypeData.BEHANDLING -> nyBehandlingAktivitet()
            AktivitetTypeData.SOKEAVTALE -> nySokeAvtaleAktivitet()
            AktivitetTypeData.JOBBSOEKING -> nyttStillingssok()
            AktivitetTypeData.EGENAKTIVITET -> nyEgenaktivitet()
            AktivitetTypeData.SAMTALEREFERAT -> nySamtaleReferat()
            AktivitetTypeData.STILLING_FRA_NAV -> nyStillingFraNavMedCVKanDeles()
            AktivitetTypeData.EKSTERNAKTIVITET -> nyEksternAktivitet()
        }
    }

    @JvmStatic
    fun nyStillingFraNavMedCVKanDeles(
        aktorId: Person.AktorId? = null,
        oppfolgingsperiode: UUID? = null
    ): StillingFraNav.Opprett {
        return StillingFraNav.Opprett(
            nyOpprettFelter(AktivitetTypeData.STILLING_FRA_NAV, oppfolgingsperiode ?: UUID.randomUUID(), aktorId ?: TestData.KJENT_AKTOR_ID),
            nyMuterbareFelter(),
            nySporingsData(aktorId),
            AktivitetTypeDataTestBuilder.nyStillingFraNav(true)
        )
    }

    @JvmStatic
    fun nyStillingFraNav(
        aktorId: Person.AktorId,
        oppfolgingsperiode: UUID? = null
    ): StillingFraNav.Opprett {
        return StillingFraNav.Opprett(
            nyOpprettFelter(AktivitetTypeData.STILLING_FRA_NAV, oppfolgingsperiode ?: UUID.randomUUID(), aktorId),
            nyMuterbareFelter(),
            nySporingsData(aktorId),
            AktivitetTypeDataTestBuilder.nyStillingFraNav(false)
        )
    }

    @JvmStatic
    fun nyttStillingssok(
        aktorId: Person.AktorId? = null,
        oppfolgingsperiode: UUID? = null,
        kontorSperreEnhet: String? = null,
    ): Jobbsoeking.Opprett {
        return Jobbsoeking.Opprett(
            nyOpprettFelter(AktivitetTypeData.JOBBSOEKING,
                oppfolgingsperiode ?: UUID.randomUUID(),
                aktorId ?: TestData.KJENT_AKTOR_ID,
                kontorSperreEnhet),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.nyttStillingssok()
        )
    }

    fun nyEgenaktivitet(): Egenaktivitet.Opprett {
        return Egenaktivitet.Opprett(
            nyOpprettFelter(AktivitetTypeData.EGENAKTIVITET),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.nyEgenaktivitet()
        )
    }

    fun nySokeAvtaleAktivitet(): Sokeavtale.Opprett {
        return Sokeavtale.Opprett(
            nyOpprettFelter(AktivitetTypeData.SOKEAVTALE),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.nySokeAvtaleAktivitet()
        )
    }

    fun nyIJobbAktivitet(): Ijobb.Opprett {
        return Ijobb.Opprett(
            nyOpprettFelter(AktivitetTypeData.IJOBB),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.nyIJobbAktivitet()
        )
    }

    fun nyBehandlingAktivitet(): Behandling.Opprett {
        return Behandling.Opprett(
            nyOpprettFelter(AktivitetTypeData.BEHANDLING),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.nyBehandlingAktivitet()
        )
    }

    fun nyMoteAktivitet(): Mote.Opprett {
        return Mote.Opprett(
            nyOpprettFelter(AktivitetTypeData.MOTE),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.moteData()
        )
    }

    fun nySamtaleReferat(): Mote.Opprett {
        val muterbareFelter = AktivitetMuterbareFelter(
            "tittel",
            "beskrivelse",
            nyDato(),
            null,  // tilDato is null for samtalereferat
            "lenke"
        )
        return Mote.Opprett(
            nyOpprettFelter(AktivitetTypeData.SAMTALEREFERAT),
            muterbareFelter,
            nySporingsData(),
            AktivitetTypeDataTestBuilder.moteData()
        )
    }

    fun nyEksternAktivitet(): Eksternaktivitet.Opprett {
        return Eksternaktivitet.Opprett(
            UUID.randomUUID(),
            false,
            nyOpprettFelter(AktivitetTypeData.EKSTERNAKTIVITET),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.eksternAktivitetData(AktivitetskortType.ARENA_TILTAK)
        )
    }

    fun nyEksternAktivitet(aktivitetskortType: AktivitetskortType?): Eksternaktivitet.Opprett {
        return Eksternaktivitet.Opprett(
            UUID.randomUUID(),
            false,
            nyOpprettFelter(AktivitetTypeData.EKSTERNAKTIVITET),
            nyMuterbareFelter(),
            nySporingsData(),
            AktivitetTypeDataTestBuilder.eksternAktivitetData(aktivitetskortType)
        )
    }

    fun nyForhaandorientering(): Forhaandsorientering? {
        return Forhaandsorientering.builder()
            .id(UUID.randomUUID().toString())
            .tekst("Lol")
            .aktivitetId("abc")
            .type(Type.SEND_PARAGRAF_11_9)
            .opprettetAv("meg")
            .opprettetDato(Date())
            .build()
    }

    fun nyDato(): Date? {
        val now = System.currentTimeMillis()
        val randomMillis = Random().nextLong(0, now)
        return DateUtils.truncate(Date(randomMillis), Calendar.SECOND)
    }

    fun nyZonedDateTime(): ZonedDateTime {
        return nyDato()!!.toInstant().atZone(ZoneId.systemDefault())
    }
}