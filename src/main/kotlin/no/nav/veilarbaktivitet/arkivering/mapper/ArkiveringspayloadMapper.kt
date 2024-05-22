package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.util.DateUtils.norskDato

object ArkiveringspayloadMapper {

    fun mapTilArkivPayload(
        arkiveringsData: ArkiveringsController.ArkiveringsData,
        sakDTO: SakDTO,
        journalførendeEnhet: String,
    ): ArkivPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(arkiveringsData.aktiviteter, arkiveringsData.dialoger, arkiveringsData.historikkForAktiviteter, arkiveringsData.arenaAktiviteter)
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
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(arkiveringsData.aktiviteter, arkiveringsData.dialoger, arkiveringsData.historikkForAktiviteter, arkiveringsData.arenaAktiviteter)

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
        historikkForAktiviteter: Map<AktivitetId, Historikk>,
        arenaAktiviteter: List<ArenaAktivitetDTO>
    ): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
        val aktivitetDialoger = dialoger.groupBy { it.aktivitetId }

        val arkivAktiviteter = aktiviteter.map {
            it.toArkivPayload(
                meldinger = meldingerTilhørendeAktivitet(aktivitetDialoger, it.id.toString()),
                historikk = historikkForAktiviteter[it.id] ?: throw RuntimeException("Fant ikke historikk på aktivitet med id ${it.id}")
            )
        }

        val arenaArkivAktiviteter = arenaAktiviteter.map {
            it.toArkivPayload(meldingerTilhørendeAktivitet(aktivitetDialoger, it.id))
        }

        val meldingerUtenAktivitet = aktivitetDialoger[null] ?: emptyList()
        return Pair(arkivAktiviteter + arenaArkivAktiviteter, meldingerUtenAktivitet.map { it.tilArkivDialogTråd() })
    }

    private fun meldingerTilhørendeAktivitet(dialoger: Map<String?, List<DialogClient. DialogTråd>>, aktivitetId: String) =
        dialoger[aktivitetId]
            ?.map { it.meldinger.map { it.tilMelding() } }
            ?.flatten() ?: emptyList()
}
