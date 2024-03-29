package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper.toAktivitetsDataInsert
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

internal class AktivitetskortMapperTest {
    fun aktivitetskort(): Aktivitetskort {
        return Aktivitetskort(
            id = UUID.randomUUID(),
            personIdent = "1234567890",
            startDato = LocalDate.now().minusDays(30),
            sluttDato = LocalDate.now().minusDays(30),
            tittel = "The Elder Scrolls: Arena",
            beskrivelse = "arenabeskrivelse",
            aktivitetStatus = AktivitetskortStatus.GJENNOMFORES,
            endretAv = Ident("arenaEndretav", Innsender.ARENAIDENT),
            endretTidspunkt = ZonedDateTime.now(),
            avtaltMedNav = false
        )
    }

    @Test
    fun should_map_list_fields_to_empty_list_if_they_are_null() {
        // These fields are set to null when deserializing, but are empty lists when using builder
        val aktivitetskortWithNullFields = aktivitetskort()
            .copy(etiketter = emptyList(), handlinger = emptyList(), detaljer = emptyList())
        val result = EksternAktivitetskortBestilling(
            aktivitetskortWithNullFields.copy(endretTidspunkt = ZonedDateTime.now()),
            "test-source",
            AktivitetskortType.ARENA_TILTAK,
            UUID.randomUUID(),
            ActionType.UPSERT_AKTIVITETSKORT_V1,
            Person.aktorId("1234567890")
        ).toAktivitetsDataInsert()
        result.withAktorId(Person.aktorId("adas"))
        Assertions.assertThat(result.getEksternAktivitetData().detaljer).isEmpty()
        Assertions.assertThat(result.getEksternAktivitetData().etiketter).isEmpty()
        Assertions.assertThat(result.getEksternAktivitetData().handlinger).isEmpty()
    }
}
