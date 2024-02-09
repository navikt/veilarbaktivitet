package no.nav.veilarbaktivitet.arkivering.mapper

import io.micrometer.core.instrument.util.StringEscapeUtils
import no.nav.veilarbaktivitet.arkivering.DialogClient
import no.nav.veilarbaktivitet.arkivering.DialogTråd
import no.nav.veilarbaktivitet.arkivering.Melding

fun DialogClient.DialogTrådDTO.tilDialogTråd() =
    DialogTråd(
        overskrift = overskrift,
        egenskaper = egenskaper.map { it.toString() },
        meldinger =  meldinger.map { it.tilMelding()
        }
    )

fun DialogClient.MeldingDTO.tilMelding() =
    Melding(
        avsender = avsender.toString(),
        sendt = "${sendt.norskDato()} kl. ${sendt.klokkeslett()}",
        lest = lest,
        viktig = viktig,
        tekst = tekst.htmlEscape()
    )


fun String.htmlEscape(): String =
    replace("\n", "<br/>")
        .let { StringEscapeUtils.escapeJson(it) }