package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.util.DateUtils.norskDato

object ArkiveringspayloadMapper {

    fun mapTilArkivPayload(
        arkiveringsData: ArkiveringsController.ArkiveringsData,
        sakDTO: SakDTO,
        journalførendeEnhet: String,
    ): ArkivPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(arkiveringsData.aktiviteter, arkiveringsData.dialoger, arkiveringsData.historikkForAktiviteter)
        return ArkivPayload(
            navn = arkiveringsData.navn.tilFornavnMellomnavnEtternavn(),
            fnr = arkiveringsData.fnr.get(),
            oppfølgingsperiodeStart = norskDato(arkiveringsData.oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = arkiveringsData.oppfølgingsperiode.sluttDato?.let { norskDato(it) },
            sakId = sakDTO.sakId,
            fagsaksystem = sakDTO.fagsaksystem,
            tema = sakDTO.tema,
            oppfølgingsperiodeId = arkiveringsData.oppfølgingsperiode.uuid,
            journalførendeEnhet = journalførendeEnhet,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = arkiveringsData.mål.mal
        )
    }

    fun mapTilForhåndsvisningsPayload(arkiveringsData: ArkiveringsController.ArkiveringsData
    ): ForhåndsvisningPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(arkiveringsData.aktiviteter, arkiveringsData.dialoger, arkiveringsData.historikkForAktiviteter)

        return ForhåndsvisningPayload(
            navn = arkiveringsData.navn.tilFornavnMellomnavnEtternavn(),
            fnr = arkiveringsData.fnr.get(),
            oppfølgingsperiodeStart = norskDato(arkiveringsData.oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = arkiveringsData.oppfølgingsperiode.sluttDato?.let { norskDato(it) },
            oppfølgingsperiodeId = arkiveringsData.oppfølgingsperiode.uuid,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = arkiveringsData.mål.mal
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
