package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.mapper.norskDato
import no.nav.veilarbaktivitet.arkivering.Metadata as ArkivMetadata
import no.nav.veilarbaktivitet.arkivering.mapper.tilDialogTråd
import no.nav.veilarbaktivitet.arkivering.mapper.tilMelding
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.ZonedDateTime
import java.util.*

object Arkiveringslogikk {

    fun lagArkivPayload(
        fnr: Fnr,
        navn: Navn,
        oppfølgingsperiode: OppfolgingPeriodeMinimalDTO,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>
    ): ArkivPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(oppfølgingsperiode.uuid, aktiviteter, dialoger)
        return ArkivPayload(
            metadata = ArkivMetadata(
                navn = navn.tilFornavnMellomnavnEtternavn(),
                fnr = fnr.get(),
                oppfølgingsperiodeStart = oppfølgingsperiode.startDato.norskDato(),
                oppfølgingsperiodeSlutt = oppfølgingsperiode.sluttDato?.norskDato()
            ),
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger
        )
    }

    private fun lagDataTilOrkivar(
        oppfølgingsperiodeId: UUID,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>
    ): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
        val aktiviteterIOppfølgingsperioden = aktiviteter.filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
        val dialogerIOppfølgingsperioden = dialoger.filter { it.oppfolgingsperiode == oppfølgingsperiodeId }

        val aktivitetDialoger = dialogerIOppfølgingsperioden.groupBy { it.aktivitetId }

        val aktiviteterPayload = aktiviteterIOppfølgingsperioden
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

    fun aktiviteterOgDialogerOppdatertEtter(
        tidspunkt: ZonedDateTime,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>
    ): Boolean {
        val aktiviteterTidspunkt = aktiviteter.map { DateUtils.dateToZonedDateTime(it.endretDato) }
        val dialogerTidspunkt = dialoger.map { it.meldinger }.flatten().map { it.sendt }
        val sistOppdatert =
            (aktiviteterTidspunkt + dialogerTidspunkt).maxOrNull() ?: ZonedDateTime.now().minusYears(100)
        return sistOppdatert > tidspunkt
    }
}
