package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.DialogClient
import no.nav.veilarbaktivitet.arkivering.ArkivDialogtråd
import no.nav.veilarbaktivitet.arkivering.Melding

fun DialogClient.DialogTråd.tilDialogTråd() =
    ArkivDialogtråd(
        overskrift = overskrift,
        egenskaper = egenskaper.map { it.toString() },
        meldinger =  meldinger.map { it.tilMelding()
        }
    )

fun DialogClient.Melding.tilMelding() =
    Melding(
        avsender = avsender.toString(),
        sendt = "${sendt.norskDato()} kl. ${sendt.klokkeslett()}",
        lest = lest,
        viktig = viktig,
        tekst = tekst
    )
