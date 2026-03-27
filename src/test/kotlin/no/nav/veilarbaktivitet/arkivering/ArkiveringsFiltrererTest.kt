package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.*
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
                ),
                AktivitetDataTestBuilder.nyttStillingssok().withStillingsSoekAktivitetData(
                    StillingsoekAktivitetData.builder().stillingsoekEtikett(
                        StillingsoekEtikettData.INNKALT_TIL_INTERVJU
                    ).build()
                ),
                AktivitetDataTestBuilder.nyttStillingssok().withStillingsSoekAktivitetData(
                    StillingsoekAktivitetData.builder().build()
                ),
            )
        )
        val filter = defaultFilter.copy(
            stillingsstatusFilter = listOf(
                ArkiveringsController.SøknadsstatusFilter.SOKNAD_SENDT,
                ArkiveringsController.SøknadsstatusFilter.FATT_JOBBEN,
                ArkiveringsController.SøknadsstatusFilter.INNKALT_TIL_INTERVJU,
            )
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(3)
        assertThat(filtrertData.aktiviteter[0].stillingFraNavData.soknadsstatus).isEqualTo(Soknadsstatus.FATT_JOBBEN)
        assertThat(filtrertData.aktiviteter[1].stillingsSoekAktivitetData.stillingsoekEtikett).isEqualTo(
            StillingsoekEtikettData.SOKNAD_SENDT
        )
        assertThat(filtrertData.aktiviteter[2].stillingsSoekAktivitetData.stillingsoekEtikett).isEqualTo(
            StillingsoekEtikettData.INNKALT_TIL_INTERVJU
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
            dialoger = listOf(defaultDialogtråd.copy(kontorsperreEnhetId = "1234"))
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
            ),
            dialoger = listOf(
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode)
                ),
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(
                        Date.from(Instant.now().minusSeconds(1000))
                    )
                ),
                defaultDialogtråd
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
        assertThat(filtrertData.dialoger).hasSize(1)
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
            ),
            dialoger = listOf(
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode)
                ),
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(
                        Date.from(Instant.now().minusSeconds(1000))
                    )
                ),
                defaultDialogtråd
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
        assertThat(filtrertData.dialoger).hasSize(3)
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
            ),
            dialoger = listOf(
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(opprettetTidspunktTilInkludertKvpPeriode)
                ),
                defaultDialogtråd.copy(
                    kontorsperreEnhetId = "1234",
                    opprettetDato = DateUtils.dateToZonedDateTime(
                        Date.from(Instant.now().minusSeconds(1000))
                    )
                ),
                defaultDialogtråd
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
        assertThat(filtrertData.dialoger).hasSize(1)
    }

    @Test
    fun `Skal kunne ekskludere aktiviteter utafor periode`() {
        val periodeStart = ZonedDateTime.now()
        val periodeSlutt = ZonedDateTime.now().plusDays(10)

        val aktivitetMedStartIPerioden = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeStart.plusDays(1).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(5).toInstant()))
            .build()
        val aktivitetMedSluttIPerioden = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeStart.minusDays(5).toInstant()))
            .tilDato(Date.from(periodeSlutt.minusDays(1).toInstant()))
            .build()
        val aktivitetUtenSluttdatoMedStartFørPerioden = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeStart.minusDays(2).toInstant()))
            .tilDato(null)
            .build()
        val aktivitetHeltFørPerioden = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeStart.minusDays(20).toInstant()))
            .tilDato(Date.from(periodeStart.minusDays(15).toInstant()))
            .build()
        val aktivitetHeltEtterPerioden = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeSlutt.plusDays(15).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(20).toInstant()))
            .build()

        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(
                aktivitetMedStartIPerioden,
                aktivitetMedSluttIPerioden,
                aktivitetUtenSluttdatoMedStartFørPerioden,
                aktivitetHeltFørPerioden,
                aktivitetHeltEtterPerioden,
            )
        )
        val filter = defaultFilter.copy(
            datoPeriode = ArkiveringsController.DatoPeriode(fra = periodeStart, til = periodeSlutt)
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(3)
    }

    @Test
    fun `Skal kunne ekskludere Arena-aktiviteter utafor periode`() {
        val periodeStart = ZonedDateTime.now()
        val periodeSlutt = ZonedDateTime.now().plusDays(10)

        val arenaAktivitetMedStartIPerioden = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeStart.plusDays(1).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(5).toInstant()))
            .build()
        val arenaAktivitetMedSluttIPerioden = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeStart.minusDays(5).toInstant()))
            .tilDato(Date.from(periodeSlutt.minusDays(1).toInstant()))
            .build()
        val arenaAktivitetUtenSluttdatoMedStartFørPerioden = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeStart.minusDays(2).toInstant()))
            .tilDato(null)
            .build()
        val arenaAktivitetHeltFørPerioden = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeStart.minusDays(20).toInstant()))
            .tilDato(Date.from(periodeStart.minusDays(15).toInstant()))
            .build()
        val arenaAktivitetHeltEtterPerioden = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeSlutt.plusDays(15).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(20).toInstant()))
            .build()

        val arkiveringsData = defaultArkiveringsData.copy(
            arenaAktiviteter = listOf(
                arenaAktivitetMedStartIPerioden,
                arenaAktivitetMedSluttIPerioden,
                arenaAktivitetUtenSluttdatoMedStartFørPerioden,
                arenaAktivitetHeltFørPerioden,
                arenaAktivitetHeltEtterPerioden,
            )
        )
        val filter = defaultFilter.copy(
            datoPeriode = ArkiveringsController.DatoPeriode(fra = periodeStart, til = periodeSlutt)
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.arenaAktiviteter).hasSize(3)
    }

    @Test
    fun `Periode slutt skal tolkes inclusive`() {
        val periodeStart = ZonedDateTime.parse("2026-03-26T23:00:00.000Z")
        // Typisk input fra frontend som tolkes som 2026-03-27T00:00:00.000 norsk tid
        // 27.03 skal tolkes inklusivt
        val periodeSlutt = ZonedDateTime.parse("2026-03-26T23:00:00.000Z")

        val aktivitetSomStarterPåSisteDag = AktivitetDataTestBuilder.nyAktivitet()
            .fraDato(Date.from(periodeSlutt.plusHours(5).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(5).toInstant()))
            .build()
        val arenaAktivitetSomStarterPåSisteDag = ArenaAktivitetDTO.builder()
            .fraDato(Date.from(periodeSlutt.plusHours(5).toInstant()))
            .tilDato(Date.from(periodeSlutt.plusDays(5).toInstant()))
            .build()
        val dialogSomStarterPåSisteDag = defaultDialogtråd.copy(
            id = "1",
            opprettetDato = periodeSlutt.plusHours(5),
            meldinger = listOf(lagMelding(periodeSlutt.plusDays(5)))
        )

        val arkiveringsData = defaultArkiveringsData.copy(
            aktiviteter = listOf(aktivitetSomStarterPåSisteDag),
            arenaAktiviteter = listOf(arenaAktivitetSomStarterPåSisteDag),
            dialoger = listOf(dialogSomStarterPåSisteDag)
        )

        val filter = defaultFilter.copy(
            datoPeriode = ArkiveringsController.DatoPeriode(fra = periodeStart, til = periodeSlutt)
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.aktiviteter).hasSize(3)
    }

    @Test
    fun `Skal kunne ekskludere dialoger utafor periode`() {
        val periodeStart = ZonedDateTime.now()
        val periodeSlutt = ZonedDateTime.now().plusDays(10)

        val dialogMedOpprettetIPerioden = defaultDialogtråd.copy(
            id = "1",
            opprettetDato = periodeStart.plusDays(1),
            meldinger = listOf(lagMelding(periodeSlutt.plusDays(5)))
        )
        val dialogMedSisteMeldingIPerioden = defaultDialogtråd.copy(
            id = "2",
            opprettetDato = periodeStart.minusDays(5),
            meldinger = listOf(lagMelding(periodeSlutt.minusDays(1)))
        )
        val dialogMedBådeOpprettetOgSisteMeldingIPerioden = defaultDialogtråd.copy(
            id = "3",
            opprettetDato = periodeStart.plusDays(2),
            meldinger = listOf(lagMelding(periodeStart.plusDays(3)))
        )
        val dialogHeltFørPerioden = defaultDialogtråd.copy(
            id = "4",
            opprettetDato = periodeStart.minusDays(20),
            meldinger = listOf(lagMelding(periodeStart.minusDays(15)))
        )
        val dialogHeltEtterPerioden = defaultDialogtråd.copy(
            id = "5",
            opprettetDato = periodeSlutt.plusDays(15),
            meldinger = listOf(lagMelding(periodeSlutt.plusDays(20)))
        )

        val arkiveringsData = defaultArkiveringsData.copy(
            dialoger = listOf(
                dialogMedOpprettetIPerioden,
                dialogMedSisteMeldingIPerioden,
                dialogMedBådeOpprettetOgSisteMeldingIPerioden,
                dialogHeltFørPerioden,
                dialogHeltEtterPerioden,
            )
        )
        val filter = defaultFilter.copy(
            datoPeriode = ArkiveringsController.DatoPeriode(fra = periodeStart, til = periodeSlutt)
        )
        val filtrertData = filtrerArkiveringsData(arkiveringsData, filter)
        assertThat(filtrertData.dialoger).hasSize(3)
    }

    val defaultFilter = ArkiveringsController.Filter(
        inkluderHistorikk = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        datoPeriode = null,
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList(),
        inkluderDialoger = true,
        kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(
            alternativ = INKLUDER_KVP_AKTIVITETER,
            start = null,
            slutt = null
        )
    )

    val defaultArkiveringsData = ArkiveringsData(
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
        arenaAktiviteter = emptyList(),
        journalførendeEnhetNavn = "Nav Helsfyr"
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
        opprettetDato = ZonedDateTime.now().minusSeconds(10),
        kontorsperreEnhetId = null,
        sisteDato = ZonedDateTime.now()
    )

    fun lagMelding(sendt: ZonedDateTime) = DialogClient.Melding(
        id = UUID.randomUUID().toString(),
        dialogId = "123",
        avsender = Avsender.VEILEDER,
        avsenderId = "421",
        sendt = sendt,
        lest = true,
        viktig = false,
        tekst = "Melding"
    )
}
