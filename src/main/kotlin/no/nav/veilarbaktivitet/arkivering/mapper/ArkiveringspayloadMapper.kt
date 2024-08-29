package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import org.slf4j.LoggerFactory

object ArkiveringspayloadMapper {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun mapTilArkivPayload(
        innsamletDataTilArkiv: ArkiveringsController.InnsamletDataTilArkiv,
        sakDTO: SakDTO,
        journalførendeEnhet: String,
    ): ArkivPayload {
        val (arkivaktiviteter, arkivdialoger) = mapDataTilOrkivar(innsamletDataTilArkiv.aktiviteter, innsamletDataTilArkiv.dialoger, innsamletDataTilArkiv.historikkForAktiviteter, innsamletDataTilArkiv.arenaAktiviteter)
        return ArkivPayload(
            navn = innsamletDataTilArkiv.navn.tilFornavnMellomnavnEtternavn(),
            fnr = innsamletDataTilArkiv.fnr.get(),
            oppfølgingsperiodeStart = norskDato(innsamletDataTilArkiv.oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = innsamletDataTilArkiv.oppfølgingsperiode.sluttDato?.let { norskDato(it) },
            sakId = sakDTO.sakId,
            fagsaksystem = sakDTO.fagsaksystem,
            tema = sakDTO.tema,
            oppfølgingsperiodeId = innsamletDataTilArkiv.oppfølgingsperiode.uuid,
            journalførendeEnhet = journalførendeEnhet,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = innsamletDataTilArkiv.mål.mal
        )
    }

    fun mapTilForhåndsvisningsPayload(innsamletDataTilArkiv: ArkiveringsController.InnsamletDataTilArkiv
    ): ForhåndsvisningPayload {
        val (arkivaktiviteter, arkivdialoger) = mapDataTilOrkivar(innsamletDataTilArkiv.aktiviteter, innsamletDataTilArkiv.dialoger, innsamletDataTilArkiv.historikkForAktiviteter, innsamletDataTilArkiv.arenaAktiviteter)

        return ForhåndsvisningPayload(
            navn = innsamletDataTilArkiv.navn.tilFornavnMellomnavnEtternavn(),
            fnr = innsamletDataTilArkiv.fnr.get(),
            oppfølgingsperiodeStart = norskDato(innsamletDataTilArkiv.oppfølgingsperiode.startDato),
            oppfølgingsperiodeSlutt = innsamletDataTilArkiv.oppfølgingsperiode.sluttDato?.let { norskDato(it) },
            oppfølgingsperiodeId = innsamletDataTilArkiv.oppfølgingsperiode.uuid,
            aktiviteter = arkivaktiviteter.groupBy { it.status },
            dialogtråder = arkivdialoger,
            mål = innsamletDataTilArkiv.mål.mal
        )
    }

    private fun mapDataTilOrkivar(
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>,
        historikkForAktiviteter: Map<AktivitetId, Historikk>,
        arenaAktiviteter: List<ArenaAktivitetDTO>
    ): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
        val aktivitetDialoger = dialoger.groupBy { it.aktivitetId }

        val arkivAktiviteter = aktiviteter.map {
            it.toArkivPayload(
                dialogtråd = dialogTilhørendeAktiviteten(aktivitetDialoger, it.id.toString()),
                historikk = historikkForAktiviteter[it.id] ?: throw RuntimeException("Fant ikke historikk på aktivitet med id ${it.id}")
            )
        }

        val arenaArkivAktiviteter = arenaAktiviteter.map {
            it.toArkivPayload(dialogTilhørendeAktiviteten(aktivitetDialoger, it.id))
        }

        val meldingerUtenAktivitet = aktivitetDialoger[null] ?: emptyList()
        return Pair(arkivAktiviteter + arenaArkivAktiviteter, meldingerUtenAktivitet.map { it.tilArkivDialogTråd() })
    }

    private fun dialogTilhørendeAktiviteten(dialoger: Map<String?, List<DialogClient. DialogTråd>>, aktivitetId: String): ArkivDialogtråd? {
        val dialogerTilhørendeAktiviteten = dialoger[aktivitetId] ?: return null

        return if (dialogerTilhørendeAktiviteten.size == 1) {
            dialogerTilhørendeAktiviteten.first().tilArkivDialogTråd()
        } else {
            if (dialogerTilhørendeAktiviteten.size > 1) logger.info("Finnes ${dialogerTilhørendeAktiviteten.size} dialogtråder til aktivitet $aktivitetId. Det skal kun være maks én.")
            null
        }
    }
}
