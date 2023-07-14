package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.ZonedDateTime
import java.util.*

object AktivitetskortMapper {
    private fun getTiltakskode(bestilling: AktivitetskortBestilling): String? {
        return if (bestilling is ArenaAktivitetskortBestilling) {
            bestilling.arenaTiltakskode
        } else if (bestilling is EksternAktivitetskortBestilling) {
            null
        } else {
            throw IllegalStateException("Unexpected value: $bestilling")
        }
    }

    private fun getArenaId(bestilling: AktivitetskortBestilling): ArenaId? {
        return if (bestilling is ArenaAktivitetskortBestilling) {
            bestilling.eksternReferanseId
        } else if (bestilling is EksternAktivitetskortBestilling) {
            null
        } else {
            throw IllegalStateException("Unexpected value: $bestilling")
        }
    }

    @JvmStatic
    fun mapTilAktivitetData(
        bestilling: AktivitetskortBestilling,
        opprettetDato: ZonedDateTime?,
        oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? = null
    ): AktivitetData {
        val (id, _, tittel, beskrivelse, aktivitetStatus, startDato, sluttDato, endretAv, endretTidspunkt, avtaltMedNav, oppgave, handlinger, detaljer, etiketter) = bestilling.aktivitetskort
        val oppfolgingsperiodeSluttDato = oppfolgingsperiode?.sluttDato?.toLocalDateTime()
        val eksternAktivitetData = EksternAktivitetData(
            source = bestilling.source,
            type = bestilling.aktivitetskortType,
            tiltaksKode = getTiltakskode(bestilling),
            arenaId = getArenaId(bestilling),
            detaljer = Optional.ofNullable(detaljer).orElse(listOf()),
            oppgave = oppgave,
            handlinger = Optional.ofNullable(handlinger).orElse(listOf()),
            etiketter = Optional.ofNullable(
                etiketter
            ).orElse(listOf()),
            opprettetSomHistorisk = oppfolgingsperiodeSluttDato != null,
            oppfolgingsperiodeSlutt = oppfolgingsperiodeSluttDato
        )

//        val aktivitetData = AktivitetData.builder()
//        aktivitetData.funksjonellId = id
//        aktivitetData.aktorId = bestilling.aktorId.get()
//        aktivitetData.avtalt = avtaltMedNav
//        aktivitetData.tittel = tittel
//        aktivitetData.fraDato = DateUtils.toDate(startDato)
//        aktivitetData.tilDato = DateUtils.toDate(sluttDato)
//        aktivitetData.beskrivelse = beskrivelse
//        aktivitetData.status = aktivitetStatus
//        aktivitetData.aktivitetType = AktivitetTypeData.EKSTERNAKTIVITET
//        aktivitetData.endretAv = endretAv!!.ident
//        aktivitetData.endretAvType = endretAv.identType.toInnsender()
//        aktivitetData.opprettetDato = DateUtils.zonedDateTimeToDate(opprettetDato)
//        aktivitetData.endretDato = DateUtils.zonedDateTimeToDate(endretTidspunkt)
//        aktivitetData.eksternAktivitetData = eksternAktivitetData
//        return aktivitetData

        return AktivitetData.builder()
            .funksjonellId(id)
            .aktorId(bestilling.aktorId.get())
            .avtalt(avtaltMedNav)
            .tittel(tittel)
            .fraDato(DateUtils.toDate(startDato))
            .tilDato(DateUtils.toDate(sluttDato))
            .beskrivelse(beskrivelse)
            .status(aktivitetStatus)
            .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
            .endretAv(endretAv!!.ident)
            .endretAvType(endretAv.identType.toInnsender())
            .opprettetDato(DateUtils.zonedDateTimeToDate(opprettetDato))
            .endretDato(DateUtils.zonedDateTimeToDate(endretTidspunkt))
            .eksternAktivitetData(eksternAktivitetData)
            .build()
    }
}
