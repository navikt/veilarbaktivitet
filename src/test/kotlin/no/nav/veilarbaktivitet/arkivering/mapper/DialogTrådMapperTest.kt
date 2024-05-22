package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.DialogClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class DialogTrådMapperTest {

    @Test
    fun `Alle meldinger er lest av bruker så indeks peker på siste melding fra veileder`() {
        val dialogTrådId = "random"
        val dialogTråd = dialogTråd(
            id = dialogTrådId,
            meldinger = listOf(
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraBruker(dialogTrådId))
        )

        val arkivDialogTråd = dialogTråd.tilDialogTråd()

        assertThat(arkivDialogTråd.indeksSistMeldingLestAvBruker).isEqualTo(4)
    }

    @Test
    fun `Kun første melding fra veileder er lest av bruker`() {
        val dialogTrådId = "random"
        val dialogTråd = dialogTråd(
            id = dialogTrådId,
            meldinger = listOf(
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraBruker(dialogTrådId))
        )

        val arkivDialogTråd = dialogTråd.tilDialogTråd()

        assertThat(arkivDialogTråd.indeksSistMeldingLestAvBruker).isEqualTo(1)
    }

    @Test
    fun `Den andre av tre meldinger fra veileder er lest av bruker`() {
        val dialogTrådId = "random"
        val dialogTråd = dialogTråd(
            id = dialogTrådId,
            meldinger = listOf(
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = true),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraBruker(dialogTrådId))
        )

        val arkivDialogTråd = dialogTråd.tilDialogTråd()

        assertThat(arkivDialogTråd.indeksSistMeldingLestAvBruker).isEqualTo(3)
    }

    @Test
    fun `Ingen meldinger er lest av bruker så indeks er null`() {
        val dialogTrådId = "random"
        val dialogTråd = dialogTråd(
            id = dialogTrådId,
            meldinger = listOf(
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraBruker(dialogTrådId),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraVeileder(dialogTrådId, lestAvBruker = false),
                meldingFraBruker(dialogTrådId))
        )

        val arkivDialogTråd = dialogTråd.tilDialogTråd()

        assertThat(arkivDialogTråd.indeksSistMeldingLestAvBruker).isNull()
    }

    private fun dialogTråd(id: String, meldinger: List<DialogClient.Melding>) = DialogClient.DialogTråd(
        id = id,
        aktivitetId = null,
        overskrift = "Dummy",
        oppfolgingsperiodeId = UUID.randomUUID(),
        meldinger = meldinger,
        egenskaper = emptyList()
    )

    private fun meldingFraBruker(dialogId: String) = melding(
        dialogId = dialogId,
        sendtAv = DialogClient.Avsender.BRUKER,
        lestAvBruker = true,
    )

    private fun meldingFraVeileder(dialogId: String, lestAvBruker: Boolean) = melding(
        dialogId = dialogId,
        sendtAv = DialogClient.Avsender.VEILEDER,
        lestAvBruker = lestAvBruker,
    )

    private fun melding(dialogId: String, sendtAv: DialogClient.Avsender, lestAvBruker: Boolean) = DialogClient.Melding(
        id = UUID.randomUUID().toString(),
        dialogId = dialogId,
        avsender = sendtAv,
        avsenderId = "DummyId",
        sendt = ZonedDateTime.now(),
        lest = true,
        viktig = false,
        tekst = "DummyTekst",
        lestAvBruker = lestAvBruker,
        lestAvBrukerTidspunkt = if (lestAvBruker) ZonedDateTime.now() else null,
    )
}