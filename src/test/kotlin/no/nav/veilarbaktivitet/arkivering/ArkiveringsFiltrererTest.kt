package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.INKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.KUN_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.DialogClient.Avsender
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.fnr
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.testutils.AktivitetTypeDataTestBuilder.eksternAktivitetData
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class ArkiveringsFiltrererTest {

    @Test
    fun `Skal kunne filtrere vekk historikk`() {
        val arkiveringsdataMedHistorikk =
            defaultArkiveringsData.copy(historikkForAktiviteter = mapOf(1L to Historikk(emptyList())))
        val filterUtenHistorikk = defaultFilter.copy(inkluderHistorikk = false)
        val filtrertData = filtrerArkiveringsData(arkiveringsdataMedHistorikk, filterUtenHistorikk)
        assertThat(filtrertData.historikkForAktiviteter).isEmpty()
    }

    @Test
    fun `Skal kunne ha historikk`() {
        val arkiveringsdataMedHistorikk =
            defaultArkiveringsData.copy(historikkForAktiviteter = mapOf(1L to Historikk(emptyList())))
        val filterMedHistorikk = defaultFilter.copy(inkluderHistorikk = true)
        val filtrertData = filtrerArkiveringsData(arkiveringsdataMedHistorikk, filterMedHistorikk)
        assertThat(filtrertData.historikkForAktiviteter).hasSize(1)
    }

    @Test
    fun `Hvis ingen filter på avtaltMedNav er valgt skal ikke filteret ha effekt`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
                AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
            )
        )
        val tomtFilter = defaultFilter.copy(aktivitetAvtaltMedNavFilter = emptyList())
        val filtrertData = filtrerArkiveringsData(arkiveringsData, tomtFilter)
        assertThat(filtrertData.aktiviteter).hasSize(2)
    }

    @Test
    fun `Skal kunne filtrere på avtaltMedNav`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
                AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
            )
        )
        val filter =
            defaultFilter.copy(aktivitetAvtaltMedNavFilter = listOf(ArkiveringsController.AvtaltMedNavFilter.AVTALT_MED_NAV))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().isAvtalt).isTrue()
    }

    @Test
    fun `Skal kunne filtrere på ikkeAvtaltMedNav`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().avtalt(true).build(),
                AktivitetDataTestBuilder.nyAktivitet().avtalt(false).build(),
            )
        )
        val filter =
            defaultFilter.copy(aktivitetAvtaltMedNavFilter = listOf(ArkiveringsController.AvtaltMedNavFilter.IKKE_AVTALT_MED_NAV))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().isAvtalt).isFalse()
    }

    @Test
    fun `Skal kunne filtrere på stillingsstatus`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyStillingFraNav().withStillingFraNavData(
                    StillingFraNavData.builder().soknadsstatus(
                        Soknadsstatus.CV_DELT
                    ).build()
                ),
                AktivitetDataTestBuilder.nyStillingFraNav().withStillingFraNavData(
                    StillingFraNavData.builder().soknadsstatus(
                        Soknadsstatus.FATT_JOBBEN
                    ).build()
                ),
                AktivitetDataTestBuilder.nyttStillingssok().withStillingsSoekAktivitetData(
                    StillingsoekAktivitetData.builder().stillingsoekEtikett(
                        StillingsoekEtikettData.SOKNAD_SENDT
                    ).build()
                )
            )
        )
        val filter = defaultFilter.copy(
            stillingsstatusFilter = listOf(
                ArkiveringsController.SøknadsstatusFilter.SOKNAD_SENDT,
                ArkiveringsController.SøknadsstatusFilter.FATT_JOBBEN
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(2)
        assertThat(filtrertData.aktiviteter[0].stillingFraNavData.soknadsstatus).isEqualTo(Soknadsstatus.FATT_JOBBEN)
        assertThat(filtrertData.aktiviteter[1].stillingsSoekAktivitetData.stillingsoekEtikett).isEqualTo(
            StillingsoekEtikettData.SOKNAD_SENDT
        )
    }

    @Test
    fun `Skal kunne filtrere på arenaAktivitetsStatus`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            arenaAktiviteter = listOf(
                ArenaAktivitetDTO.builder().etikett(ArenaStatusEtikettDTO.INFOMOETE).build(),
                ArenaAktivitetDTO.builder().etikett(ArenaStatusEtikettDTO.VENTELISTE).build(),
                ArenaAktivitetDTO.builder().etikett(ArenaStatusEtikettDTO.JATAKK).build(),
                ArenaAktivitetDTO.builder().etikett(ArenaStatusEtikettDTO.AVSLAG).build(),
            )
        )
        val filter = defaultFilter.copy(
            arenaAktivitetStatusFilter = listOf(
                ArenaStatusEtikettDTO.INFOMOETE,
                ArenaStatusEtikettDTO.JATAKK
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.arenaAktiviteter).hasSize(2)
        assertThat(filtrertData.arenaAktiviteter[0].etikett).isEqualTo(ArenaStatusEtikettDTO.INFOMOETE)
        assertThat(filtrertData.arenaAktiviteter[1].etikett).isEqualTo(ArenaStatusEtikettDTO.JATAKK)
    }

    @Test
    fun `Skal kunne filtrere på AktivitetType`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyStillingFraNav().withStillingFraNavData(
                    StillingFraNavData.builder().soknadsstatus(
                        Soknadsstatus.CV_DELT
                    ).build()
                ),
                AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.SAMTALEREFERAT).build(),
                AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.JOBBSOEKING).build(),
                AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.BEHANDLING).build(),
                AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.IJOBB).build(),
                AktivitetDataTestBuilder.nyEksternAktivitet().withEksternAktivitetData(
                    eksternAktivitetData(
                        AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
                    )
                ),
                AktivitetDataTestBuilder.nyEksternAktivitet().withEksternAktivitetData(
                    eksternAktivitetData(
                        AktivitetskortType.REKRUTTERINGSTREFF
                    )
                ),
                AktivitetDataTestBuilder.nyEksternAktivitet().withEksternAktivitetData(
                    eksternAktivitetData(
                        AktivitetskortType.ARENA_TILTAK
                    )
                ),
            ),
            arenaAktiviteter = listOf(ArenaAktivitetDTO.builder().etikett(ArenaStatusEtikettDTO.AVSLAG).build())
        )
        val filter = defaultFilter.copy(
            aktivitetTypeFilter = listOf(
                ArkiveringsController.AktivitetTypeFilter.ARENA_TILTAK,
                ArkiveringsController.AktivitetTypeFilter.MIDLERTIDIG_LONNSTILSKUDD,
                ArkiveringsController.AktivitetTypeFilter.SAMTALEREFERAT,
                ArkiveringsController.AktivitetTypeFilter.STILLING,
            )
        )

        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)

        assertThat(filtrertData.arenaAktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter).hasSize(4)
        assertThat(filtrertData.aktiviteter[0].aktivitetType).isEqualTo(AktivitetTypeData.SAMTALEREFERAT)
        assertThat(filtrertData.aktiviteter[1].aktivitetType).isEqualTo(AktivitetTypeData.JOBBSOEKING) // JOBBSOKING mappes til STILLING
        assertThat(filtrertData.aktiviteter[2].eksternAktivitetData.type).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
        assertThat(filtrertData.aktiviteter[3].eksternAktivitetData.type).isEqualTo(AktivitetskortType.ARENA_TILTAK)
    }

    @Test
    fun `Skal kunne filtrere vekk dialoger`() {
        val arkiveringsData = defaultArkiveringsData.copy(dialoger = listOf(defaultDialogtråd))
        val filter = defaultFilter.copy(inkluderDialoger = false)
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.dialoger).isEmpty()
    }

    @Test
    fun `Skal kunne inkludere alle aktiviteter og dialoger inkludert aktiviteter og dialoger under KVP`() {
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(Date.from(Instant.now())).build(),
                AktivitetDataTestBuilder.nyAktivitet().build(),
            ),
            dialoger = listOf(defaultDialogtråd)
        )
        val filter =
            defaultFilter.copy(kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(alternativ = INKLUDER_KVP_AKTIVITETER))
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(2)
        assertThat(filtrertData.dialoger).hasSize(1)
    }

    @Test
    fun `Skal kunne inkludere kun kvpAktiviteter og kvpDialoger i gitt periode`() {
        val opprettetTidspunktTilInkludertKvpPeriode = Date.from(Instant.now())
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(opprettetTidspunktTilInkludertKvpPeriode).build(),
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(Date.from(Instant.now().minusSeconds(1000))).build(),
                AktivitetDataTestBuilder.nyAktivitet().build(),
            )
        )
        val filter = defaultFilter.copy(
            kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
                alternativ = KUN_KVP_AKTIVITETER,
                start = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).minusSeconds(1),
                slutt = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).plusSeconds(1)
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
        assertThat(filtrertData.aktiviteter.first().opprettetDato).isEqualTo(opprettetTidspunktTilInkludertKvpPeriode)
    }

    @Test
    fun `Skal kunne inkludere kvpAktiviteter og andre aktiviteter i gitt periode`() {
        val opprettetTidspunktTilInkludertKvpPeriode = Date.from(Instant.now())
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(opprettetTidspunktTilInkludertKvpPeriode).build(),
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(Date.from(Instant.now().minusSeconds(1000))).build(),
                AktivitetDataTestBuilder.nyAktivitet().build(),
            )
        )
        val filter = defaultFilter.copy(
            kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
                alternativ = INKLUDER_KVP_AKTIVITETER,
                start = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).minusSeconds(1),
                slutt = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).plusSeconds(1)
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(3)
    }

    @Test
    fun `Skal kunne ekskludere kvpAktiviteter`() {
        val opprettetTidspunktTilInkludertKvpPeriode = Date.from(Instant.now())
        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(opprettetTidspunktTilInkludertKvpPeriode).build(),
                AktivitetDataTestBuilder.nyAktivitet().kontorsperreEnhetId("1234")
                    .opprettetDato(Date.from(Instant.now().minusSeconds(1000))).build(),
                AktivitetDataTestBuilder.nyAktivitet().build(),
            )
        )
        val filter = defaultFilter.copy(
            kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
                alternativ = EKSKLUDER_KVP_AKTIVITETER,
                start = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).minusSeconds(1),
                slutt = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode).plusSeconds(1)
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(1)
    }

    val defaultFilter = ArkiveringsController.Filter(
        inkluderHistorikk = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList(),
        inkluderDialoger = true,
        kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
            alternativ = INKLUDER_KVP_AKTIVITETER,
            start = null,
            slutt = null
        )
    )

    val defaultArkiveringsData = ArkiveringsController.ArkiveringsData(
        fnr = fnr("12345678901"),
        navn = Navn(fornavn = "Ola", mellomnavn = "", etternavn = "Nordmann"),
        tekstTilBruker = "En tekst til bruker",
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

    val defaultDialogtråd = DialogClient.DialogTråd(
        id = "123",
        aktivitetId = "1234",
        overskrift = "Overskrift",
        oppfolgingsperiodeId = UUID.randomUUID(),
        meldinger = listOf(
            DialogClient.Melding(
                id = "12344",
                dialogId = "123",
                avsender = Avsender.VEILEDER,
                avsenderId = "421",
                sendt = ZonedDateTime.now(),
                lest = true,
                viktig = true,
                tekst = "Hei",
            )
        ),
        egenskaper = emptyList(),
        erLestAvBruker = true,
        lestAvBrukerTidspunkt = ZonedDateTime.now(),
    )
}