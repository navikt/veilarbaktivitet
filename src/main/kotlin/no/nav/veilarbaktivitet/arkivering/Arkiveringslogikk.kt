package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.mapper.tilDialogTråd
import no.nav.veilarbaktivitet.arkivering.mapper.tilMelding
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import java.time.ZonedDateTime
import java.util.*

object Arkiveringslogikk {

    fun mapTilArkivPayload(
        arkiveringsData: ArkiveringsController.ArkiveringsData,
        sakDTO: SakDTO,
        journalførendeEnhet: String,
    ): ArkivPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(oppfølgingsperiode.uuid, aktiviteter, dialoger, historikkForAktiviteter)
        return ArkivPayload(
            navn = navn.tilFornavnMellomnavnEtternavn(),
            fnr = fnr.get(),
            oppfølgingsperiodeStart = norskDato(oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = oppfølgingsperiode.sluttDato?.let { norskDato(oppfølgingsperiode.sluttDato) },
            sakId = sakDTO.sakId,
            fagsaksystem = sakDTO.fagsaksystem,
            tema = sakDTO.tema,
            oppfølgingsperiodeId = oppfølgingsperiode.uuid,
            journalførendeEnhet = journalførendeEnhet,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = mål.mal
        )
    }

    fun mapTilForhåndsvisningsPayload(arkiveringsData: ArkiveringsController.ArkiveringsData
    ): ForhåndsvisningPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(oppfølgingsperiode.uuid, aktiviteter, dialoger, historikkForAktiviteter)

        return ForhåndsvisningPayload(
            navn = navn.tilFornavnMellomnavnEtternavn(),
            fnr = fnr.get(),
            oppfølgingsperiodeStart = norskDato(oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = oppfølgingsperiode.sluttDato?.let { norskDato(oppfølgingsperiode.sluttDato) },
            oppfølgingsperiodeId = oppfølgingsperiode.uuid,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = mål.mal
        )
    }

    private fun lagDataTilOrkivar(
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>,
        historikkForAktiviteter: Map<AktivitetId, Historikk>
    ): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
        val aktivitetDialoger = dialoger.groupBy { it.aktivitetId }

        val aktiviteterPayload = aktiviteter
            .map { it ->
                val meldingerTilhørendeAktiviteten = aktivitetDialoger[it.id.toString()]?.map {
                    it.meldinger.map { it.tilMelding() }
                }?.flatten() ?: emptyList()

                it.toArkivPayload(
                    meldinger = meldingerTilhørendeAktiviteten,
                    historikk = historikkForAktiviteter[it.id] ?: throw RuntimeException("Fant ikke historikk på aktivitet med id ${it.id}")
                )
            }

        val meldingerUtenAktivitet = aktivitetDialoger[null] ?: emptyList()
        return Pair(aktiviteterPayload, meldingerUtenAktivitet.map { it.tilDialogTråd() })
    }
}
