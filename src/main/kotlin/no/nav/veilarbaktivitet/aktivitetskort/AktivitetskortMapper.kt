package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetMuterbareFelter
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Eksternaktivitet
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.ArenaAktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.EksternAktivitetskortBestilling
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.util.DateUtils
import java.time.ZoneId
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

    fun ArenaAktivitetskortBestilling.toAktivitetsDataInsert(): AktivitetData {
        return this.toAktivitet(this.aktivitetskort.endretTidspunkt, this.oppfolgingsperiodeSlutt)
            .withOppfolgingsperiodeId(this.oppfolgingsperiode)
    }

    fun EksternAktivitetskortBestilling.toAktivitetsDataInsert(): AktivitetData {
        return this.toAktivitet(this.aktivitetskort.endretTidspunkt, null)
    }

    fun AktivitetskortBestilling.toAktivitetsDataUpdate(aktivitetId: Long, versjon: Long): Eksternaktivitet.Endre {
        return this.toAktivitetsEndring(aktivitetId, versjon)
    }

    private fun AktivitetskortBestilling.toAktivitetsEndring(aktivitetId: Long, versjon: Long): Eksternaktivitet.Endre {
        val endretAv = this.aktivitetskort.endretAv
        val sporing = SporingsData(
            endretAv.ident,
            endretAv.identType.toInnsender(),
            ZonedDateTime.now()
        )

        return Eksternaktivitet.Endre(
            id = aktivitetId,
            versjon = versjon,
            muterbareFelter = this.toMuterbareFelter(),
            sporing = sporing,
            eksternAktivitetData = this.getEksternAktivitetData(null),
            erAvtalt = this.aktivitetskort.avtaltMedNav,
            status = this.aktivitetskort.aktivitetStatus.toAktivitetStatus()
        )
    }

    private fun AktivitetskortBestilling.toAktivitet(
        opprettetDato: ZonedDateTime?,
        historiskTidspunkt: ZonedDateTime?
    ): AktivitetData {
        val (id, _, tittel, beskrivelse, aktivitetStatus, startDato, sluttDato, endretAv, _, avtaltMedNav, _, _, _, _) = this.aktivitetskort
        val eksternAktivitetData = this.getEksternAktivitetData(historiskTidspunkt)

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
            .endretAv(endretAv.ident)
            .endretAvType(endretAv.identType.toInnsender())
            .opprettetDato(DateUtils.zonedDateTimeToDate(opprettetDato))
            .endretDato(DateUtils.zonedDateTimeToDate(ZonedDateTime.now()))
            .eksternAktivitetData(eksternAktivitetData)
            .build()
    }

    fun AktivitetskortBestilling.toMuterbareFelter(): AktivitetMuterbareFelter {
        return AktivitetMuterbareFelter(
            tittel = this.aktivitetskort.tittel,
            beskrivelse = this.aktivitetskort.tittel,
            fraDato = DateUtils.zonedDateTimeToDate(this.aktivitetskort.startDato?.atStartOfDay(ZoneId.systemDefault())),
            tilDato = DateUtils.zonedDateTimeToDate(this.aktivitetskort.sluttDato?.atStartOfDay(ZoneId.systemDefault())),
            lenke = null
        )
    }

    fun AktivitetskortBestilling.getEksternAktivitetData(historiskTidspunkt: ZonedDateTime?): EksternAktivitetData {
        return EksternAktivitetData(
            source = this.source,
            type = this.aktivitetskortType,
            tiltaksKode = getTiltakskode(this),
            arenaId = getArenaId(this),
            detaljer = Optional.ofNullable(this.aktivitetskort.detaljer).orElse(listOf()),
            oppgave = this.aktivitetskort.oppgave,
            handlinger = Optional.ofNullable(this.aktivitetskort.handlinger).orElse(listOf()),
            etiketter = Optional.ofNullable(this.aktivitetskort.etiketter).orElse(listOf()),
            opprettetSomHistorisk = historiskTidspunkt != null,
            oppfolgingsperiodeSlutt = historiskTidspunkt?.toLocalDateTime(),
            endretTidspunktKilde = this.aktivitetskort.endretTidspunkt
        )
    }
}
