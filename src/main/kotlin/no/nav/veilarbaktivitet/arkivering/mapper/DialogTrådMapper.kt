package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.DialogClient
import no.nav.veilarbaktivitet.arkivering.ArkivDialogtråd
import no.nav.veilarbaktivitet.arkivering.Melding
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.*


fun DialogClient.DialogTråd.tilArkivDialogTråd(): ArkivDialogtråd {
    val indeksSistLestMelding = this.indexSisteMeldingFraVeilederSomErLestAvBruker()
    val tidspunktSistLestAvBruker = indeksSistLestMelding?.let {
        val tidspunkt = lestAvBrukerTidspunkt
        DateUtils.norskDatoOgKlokkeslett(tidspunkt)
    }

    return ArkivDialogtråd(
        overskrift = overskrift,
        egenskaper = egenskaper.map { it.toString() },
        meldinger =  meldinger.map { it.tilMelding() },
        indexSisteMeldingLestAvBruker = indeksSistLestMelding,
        tidspunktSistLestAvBruker =  tidspunktSistLestAvBruker,
    )
}

fun DialogClient.Melding.tilMelding() =
    Melding(
        avsender = avsender.toString(),
        sendt = norskDatoOgKlokkeslett(sendt),
        lest = lest,
        viktig = viktig,
        tekst = tekst
    )

private fun DialogClient.DialogTråd.indexSisteMeldingFraVeilederSomErLestAvBruker(): Int? {
    if (lestAvBrukerTidspunkt == null) return null
    val indeks = meldinger.indexOfLast {
        melding -> melding.avsender == DialogClient.Avsender.VEILEDER && melding.sendt.isBefore(lestAvBrukerTidspunkt)
    }
    return if (indeks == -1) null else indeks
}