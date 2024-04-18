package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.mapper.tilDialogTråd
import no.nav.veilarbaktivitet.arkivering.mapper.tilMelding
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import java.time.ZonedDateTime
import java.util.*

object Arkiveringslogikk {

    fun lagArkivPayload(
        fnr: Fnr,
        navn: Navn,
        oppfølgingsperiode: OppfolgingPeriodeMinimalDTO,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>,
        sakDTO: SakDTO,
        mål: MålDTO,
        historikkForAktiviteter: Map<AktivitetId, Historikk>,
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

    fun lagForhåndsvisningPayload(
        fnr: Fnr,
        navn: Navn,
        oppfølgingsperiode: OppfolgingPeriodeMinimalDTO,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>,
        mål: MålDTO,
        historikkForAktiviteter: Map<AktivitetId, Historikk>,
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
        oppfølgingsperiodeId: UUID,
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>,
        historikkForAktiviteter: Map<AktivitetId, Historikk>
    ): Pair<List<ArkivAktivitet>, List<ArkivDialogtråd>> {
        val aktiviteterIOppfølgingsperioden = aktiviteter.filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
        val dialogerIOppfølgingsperioden = dialoger.filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }

        val aktivitetDialoger = dialogerIOppfølgingsperioden.groupBy { it.aktivitetId }

        val aktiviteterPayload = aktiviteterIOppfølgingsperioden
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
