package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.mapper.tilDialogTråd
import no.nav.veilarbaktivitet.arkivering.mapper.tilMelding
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

fun lagDataTilOrkivar(oppfølgingsperiodeId: UUID, aktiviteter: List<AktivitetData>, dialoger: List<DialogClient.DialogTråd>): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
    val aktivitetDialoger = dialoger.groupBy { it.aktivitetId }

    val aktiviteterPayload = aktiviteter
        .map { it ->
            val meldingerTilhørendeAktiviteten = aktivitetDialoger[it.id.toString()]?.map {
                it.meldinger.map { it.tilMelding() }
            }?.flatten() ?: emptyList()

            it.toArkivPayload(
                meldinger = meldingerTilhørendeAktiviteten
            )
        }

    val meldingerUtenAktivitet = aktivitetDialoger[null] ?: emptyList()
    return Pair(aktiviteterPayload, meldingerUtenAktivitet.map { it.tilDialogTråd() })
}

fun aktiviteterOgDialogerOppdatertEtter(tidspunkt: ZonedDateTime, aktiviteter: List<AktivitetData>, dialoger: List<DialogClient.DialogTråd>): Boolean {
    val aktiviteterTidspunkt = aktiviteter.map { DateUtils.dateToZonedDateTime(it.endretDato) }
    val dialogerTidspunkt = dialoger.map { it.meldinger }.flatten().map { it.sendt }
    val sistOppdatert = (aktiviteterTidspunkt + dialogerTidspunkt).maxOrNull() ?: ZonedDateTime.now().minusYears(100)
    return sistOppdatert > tidspunkt
}