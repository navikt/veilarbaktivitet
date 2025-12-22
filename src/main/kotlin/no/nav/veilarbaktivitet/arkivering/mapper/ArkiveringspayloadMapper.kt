package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.common.types.identer.EnhetId
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
        pdfPayload: PdfPayload,
        sakDTO: SakDTO,
        journalførendeEnhetId: EnhetId,
        tema: String,
        uuidCachetPdf: String,
        ): JournalføringPayload {
        return JournalføringPayload(
            sakId = sakDTO.sakId,
            fagsaksystem = sakDTO.fagsaksystem,
            tema = tema,
            journalførendeEnhetId = journalførendeEnhetId.get(),
            pdfPayload = pdfPayload,
            uuidCachetPdf = uuidCachetPdf,
        )
    }

    fun mapTilPdfPayload(arkiveringsData: ArkiveringsData, tekstTilBruker: String?, filter: ArkiveringsController.Filter?): PdfPayload {
        val (arkivaktiviteter, arkivdialoger) = lagDataTilOrkivar(arkiveringsData.aktiviteter, arkiveringsData.dialoger, arkiveringsData.historikkForAktiviteter, arkiveringsData.arenaAktiviteter)

        return PdfPayload(
            navn = arkiveringsData.navn.tilFornavnMellomnavnEtternavn(),
            fnr = arkiveringsData.fnr.get(),
            brukteFiltre = filter?.mapTilBrukteFiltre() ?: emptyMap(),
            tekstTilBruker = tekstTilBruker,
            journalførendeEnhetNavn = arkiveringsData.journalførendeEnhetNavn,
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
                dialogtråd = dialogTilhørendeAktiviteten(aktivitetDialoger, it.id.toString()),
                historikk = historikkForAktiviteter[it.id] ?: Historikk(emptyList())
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
