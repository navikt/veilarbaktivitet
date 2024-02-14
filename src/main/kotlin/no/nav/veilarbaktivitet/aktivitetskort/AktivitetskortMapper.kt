package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.arena.model.ArenaId
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
    fun AktivitetskortBestilling.toAktivitetsDataInsert(
        opprettet: ZonedDateTime,
        historiskTidspunkt: ZonedDateTime?
    ): AktivitetData {
        return this.toAktivitet(opprettet, historiskTidspunkt)
    }

    @JvmStatic
    fun AktivitetskortBestilling.toAktivitetsDataUpdate(): AktivitetData {
        return this.toAktivitet(null, null)
    }


    private fun AktivitetskortBestilling.toAktivitet(
        opprettetDato: ZonedDateTime?,
        historiskTidspunkt: ZonedDateTime?
    ): AktivitetData {
        val (id, _, tittel, beskrivelse, aktivitetStatus, startDato, sluttDato, endretAv, endretTidspunkt, avtaltMedNav, oppgave, handlinger, detaljer, etiketter) = this.aktivitetskort
        val eksternAktivitetData = EksternAktivitetData(
            source = this.source,
            type = this.aktivitetskortType,
            tiltaksKode = getTiltakskode(this),
            arenaId = getArenaId(this),
            detaljer = Optional.ofNullable(detaljer).orElse(listOf()),
            oppgave = oppgave,
            handlinger = Optional.ofNullable(handlinger).orElse(listOf()),
            etiketter = Optional.ofNullable(
                etiketter
            ).orElse(listOf()),
            opprettetSomHistorisk = historiskTidspunkt != null,
            oppfolgingsperiodeSlutt = historiskTidspunkt?.toLocalDateTime()
        )

        return AktivitetData.builder()
            .funksjonellId(id)
            .aktorId(this.aktorId)
            .avtalt(avtaltMedNav)
            .tittel(tittel)
            .fraDato(DateUtils.toDate(startDato))
            .tilDato(DateUtils.toDate(sluttDato))
            .beskrivelse(beskrivelse)
            .status(aktivitetStatus.toAktivitetStatus())
            .aktivitetType(AktivitetTypeData.EKSTERNAKTIVITET)
            .endretAv(endretAv!!.ident)
            .endretAvType(endretAv.identType.toInnsender())
            .opprettetDato(DateUtils.zonedDateTimeToDate(opprettetDato))
            .endretDato(DateUtils.zonedDateTimeToDate(endretTidspunkt))
            .eksternAktivitetData(eksternAktivitetData)
            .build()
    }
}
