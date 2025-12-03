package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.fnr
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.testutils.AktivitetTypeDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class ArkiveringsFiltrererTest {

    @Test
    fun `Skal kunne filtrere vekk historikk`() {
        val arkiveringsdataMedHistorikk = defaultArkiveringsData.copy(historikkForAktiviteter = mapOf( 1L to Historikk(emptyList())))
        val filterUtenHistorikk = defaultFilter.copy(inkluderHistorikk = false)
        val filtrertData = filtrerArkiveringsData(arkiveringsdataMedHistorikk, filterUtenHistorikk)
        assertThat(filtrertData.historikkForAktiviteter).isEmpty()
    }

    @Test
    fun `Skal kunne ha historikk`() {
        val arkiveringsdataMedHistorikk = defaultArkiveringsData.copy(historikkForAktiviteter = mapOf( 1L to Historikk(emptyList())))
        val filterMedHistorikk = defaultFilter.copy(inkluderHistorikk = true)
        val filtrertData = filtrerArkiveringsData(arkiveringsdataMedHistorikk, filterMedHistorikk)
        assertThat(filtrertData.historikkForAktiviteter).hasSize(1)
    }

    @Test
    fun `Hvis ingen filter på avtaltMedNav er valgt skal ikke filteret ha effekt`() {
        val arkiveringsData = defaultArkiveringsData.copy(aktiviteter = listOf(
            AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
            AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
        ))
        val tomtFilter = defaultFilter.copy(aktivitetAvtaltMedNavFilter = emptyList())
        val filtrertData = filtrerArkiveringsData(arkiveringsData, tomtFilter)
        assertThat(filtrertData.aktiviteter).hasSize(2)
    }

    @Test
    fun `Skal kunne filtrere på avtaltMedNav`() {
        val arkiveringsData = defaultArkiveringsData.copy(aktiviteter = listOf(
            AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
            AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
        ))
        val filter = defaultFilter.copy(aktivitetAvtaltMedNavFilter = listOf(ArkiveringsController.AvtaltMedNavFilter.AVTALT_MED_NAV))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().isAvtalt).isTrue()
    }

    @Test
    fun `Skal kunne filtrere på ikkeaAvtaltMedNav`() {
        val arkiveringsData = defaultArkiveringsData.copy(aktiviteter = listOf(
            AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
            AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
        ))
        val filter = defaultFilter.copy(aktivitetAvtaltMedNavFilter = listOf(ArkiveringsController.AvtaltMedNavFilter.IKKE_AVTALT_MED_NAV))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().isAvtalt).isFalse()
    }

    @Test
    fun `Skal kunne filtrere på stillingsstatus`() {
        val arkiveringsData = defaultArkiveringsData.copy(aktiviteter = listOf(
            AktivitetDataTestBuilder.nyStillingFraNav().withStillingFraNavData(StillingFraNavData.builder().soknadsstatus(
                Soknadsstatus.CV_DELT).build()),
                    AktivitetDataTestBuilder.nyStillingFraNav().withStillingFraNavData(StillingFraNavData.builder().soknadsstatus(
                Soknadsstatus.FATT_JOBBEN).build())
        ))
        val filter = defaultFilter.copy(stillingsstatusFilter = listOf(Soknadsstatus.FATT_JOBBEN))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().stillingFraNavData.soknadsstatus).isEqualTo(Soknadsstatus.FATT_JOBBEN)
    }

    val defaultFilter = ArkiveringsController.Filter(
        inkluderHistorikk = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList()
    )

    val defaultArkiveringsData = ArkiveringsController.ArkiveringsData(
        fnr = fnr("12345678901"),
        navn = Navn(fornavn = "Ola", mellomnavn = "", etternavn = "Nordmann"),
        oppfølgingsperiode = OppfolgingPeriodeMinimalDTO(
            UUID.randomUUID(),
             ZonedDateTime.now().minusMonths(2),
            ZonedDateTime.now().minusDays(10),
        ),
        aktiviteter = emptyList(),
        dialoger = emptyList(),
        mål = MålDTO("Måltekst"),
        historikkForAktiviteter = emptyMap(),
        arenaAktiviteter = emptyList()
    )
}