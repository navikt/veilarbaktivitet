package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.DialogClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class DialogTrådMapperTest {

    @Test
    fun `Alle meldinger er lest av bruker så indeks peker på siste melding fra veileder`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusDays(1)
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraBruker(),
                meldingFraVeileder(lestAvBrukerTidspunkt.minusDays(10)),
                meldingFraBruker(),
                meldingFraVeileder(lestAvBrukerTidspunkt.minusDays(9)),
                meldingFraVeileder(lestAvBrukerTidspunkt.minusDays(8)),
                meldingFraBruker())
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isEqualTo(4)
    }

    @Test
    fun `Kun første melding fra veileder er lest av bruker`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusDays(1)
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(1)),
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.plusHours(1)),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.plusHours(2)),
                meldingFraBruker())
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isEqualTo(1)
    }

    @Test
    fun `Første melding er fra veileder og kun denne er lest av bruker`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusMonths(2)
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusHours(2)),
                meldingFraBruker(sendt = lestAvBrukerTidspunkt.minusSeconds(5)),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.plusHours(1)),
            )
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isEqualTo(0)
    }

    @Test
    fun `Den andre av tre meldinger fra veileder er lest av bruker`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusDays(1)
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(3)),
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(2)),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.plusHours(1)),
                meldingFraBruker())
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isEqualTo(3)
    }

    @Test
    fun `Alle meldinger lest, nest siste er fra veileder, siste er fra bruker, indeks peker på veileders melding`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusDays(1)
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(3)),
                meldingFraBruker(sendt = lestAvBrukerTidspunkt.minusDays(2))
             )
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isEqualTo(0)
    }

    @Test
    fun `Ikke se på lestAvBruker-feltet for beregning av sist lest av bruker`() {
        val lestAvBrukerTidspunkt = ZonedDateTime.now().minusDays(1)

        val dialogTråd = dialogTråd(
            lestAvBruker = false,
            lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
            meldinger = listOf(
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(1)),
                meldingFraBruker(),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(1)),
                meldingFraVeileder(sendt = lestAvBrukerTidspunkt.minusDays(1)),
                meldingFraBruker())
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isNotNull()
    }

    @Test
    fun `Ingen meldinger er lest av bruker så indeks er null`() {
        val dialogTråd = dialogTråd(
            lestAvBruker = false,
            lestAvBrukerTidspunkt = null,
            meldinger = listOf(
                meldingFraBruker(),
                meldingFraVeileder(sendt = ZonedDateTime.now().minusDays(1)),
                meldingFraBruker(),
                meldingFraVeileder(sendt = ZonedDateTime.now().minusDays(1)),
                meldingFraVeileder(sendt = ZonedDateTime.now().minusDays(1)),
                meldingFraBruker())
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.indexSisteMeldingLestAvBruker).isNull()
    }

    @Test
    fun `Tidspunkt for indexSisteMeldingLestAvBruker skal være  riktig`() {
        val tidspunktLest = ZonedDateTime.of(LocalDate.of(2023, 2, 2), LocalTime.of(14, 12, 12, 12), ZoneId.systemDefault())
        val dialogTråd = dialogTråd(
            lestAvBruker = true,
            lestAvBrukerTidspunkt = tidspunktLest,
            meldinger = listOf(
                meldingFraVeileder(sendt = tidspunktLest.minusDays(1))
            )
        )

        val arkivDialogTråd = dialogTråd.tilArkivDialogTråd()

        assertThat(arkivDialogTråd.tidspunktSistLestAvBruker).isEqualTo("2. februar 2023 kl. 14.12")
    }

    private fun dialogTråd(meldinger: List<DialogClient.Melding>, lestAvBruker: Boolean, lestAvBrukerTidspunkt: ZonedDateTime?) = DialogClient.DialogTråd(
        id = "random",
        aktivitetId = null,
        overskrift = "Dummy",
        oppfolgingsperiodeId = UUID.randomUUID(),
        meldinger = meldinger,
        egenskaper = emptyList(),
        erLestAvBruker = lestAvBruker,
        lestAvBrukerTidspunkt = lestAvBrukerTidspunkt,
        opprettetDato = ZonedDateTime.now().minusSeconds(10),
        kontorsperreEnhetId = null,
        sisteDato = ZonedDateTime.now()
    )

    private fun meldingFraBruker(sendt: ZonedDateTime = ZonedDateTime.now()) = melding(
        sendtAv = DialogClient.Avsender.BRUKER,
        sendt = sendt
    )

    private fun meldingFraVeileder(sendt: ZonedDateTime) = melding(
        sendtAv = DialogClient.Avsender.VEILEDER,
        sendt = sendt
    )

    private fun melding(sendtAv: DialogClient.Avsender, sendt: ZonedDateTime) = DialogClient.Melding(
        id = UUID.randomUUID().toString(),
        dialogId = "random",
        avsender = sendtAv,
        avsenderId = "DummyId",
        sendt = sendt,
        lest = true,
        viktig = false,
        tekst = "DummyTekst",
    )
}