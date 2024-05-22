package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.DialogClient
import no.nav.veilarbaktivitet.arkivering.ArkivDialogtråd
import no.nav.veilarbaktivitet.arkivering.Melding
import no.nav.veilarbaktivitet.util.DateUtils.klokkeslett
import no.nav.veilarbaktivitet.util.DateUtils.norskDato

fun DialogClient.DialogTråd.tilDialogTråd() =
    ArkivDialogtråd(
        overskrift = overskrift,
        egenskaper = egenskaper.map { it.toString() },
        r meldinger =  meldinger.map { it.tilMelding()
        }
    )

fun DialogClient.Melding.tilMelding() =
    Melding(
        avsender = avsender.toString(),
        sendt = "${norskDato(sendt)} kl. ${klokkeslett(sendt)}",
        lest = lest,
        viktig = viktig,
        tekst = tekst
    )
