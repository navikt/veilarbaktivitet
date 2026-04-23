package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetBareOpprettFelter
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

    fun ArenaAktivitetskortBestilling.toAktivitetsDataInsert(oppfolgingsperiode: UUID): Eksternaktivitet.Opprett {
        return this.toAktivitet(this.aktivitetskort.endretTidspunkt, this.oppfolgingsperiodeSlutt, oppfolgingsperiode)
    }

    fun EksternAktivitetskortBestilling.toAktivitetsDataInsert(oppfolgingsperiode: UUID): Eksternaktivitet.Opprett {
        return this.toAktivitet(this.aktivitetskort.endretTidspunkt, null, oppfolgingsperiode)
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
        opprettetDato: ZonedDateTime,
        historiskTidspunkt: ZonedDateTime?,
        oppfolgingsperiode: UUID,
    ): Eksternaktivitet.Opprett {
        val (id, _, tittel, beskrivelse, aktivitetStatus, startDato, sluttDato, endretAv, _, avtaltMedNav, _, _, _, _) = this.aktivitetskort
        val eksternAktivitetData = this.getEksternAktivitetData(historiskTidspunkt)

        val opprettFelter = AktivitetBareOpprettFelter(
            aktorId = aktorId,
            aktivitetType = AktivitetTypeData.EKSTERNAKTIVITET,
            status = aktivitetStatus.toAktivitetStatus(),
            malid = null,
            kontorsperreEnhetId = null,
            opprettetDato = opprettetDato,
            automatiskOpprettet = false,
            oppfolgingsperiodeId = oppfolgingsperiode,
        )
        val muterbareFelter = AktivitetMuterbareFelter(
            tittel = tittel,
            fraDato = DateUtils.toDate(startDato),
            tilDato = DateUtils.toDate(sluttDato),
            beskrivelse = beskrivelse,
            lenke = null
        )
        val sporing = SporingsData(
            endretAv.ident,
            endretAv.identType.toInnsender(),
            endretDato = ZonedDateTime.now()
        )
        return Eksternaktivitet.Opprett(
            id,
            avtaltMedNav,
            opprettFelter,
            muterbareFelter,
            sporing,
            eksternAktivitetData,
        )
    }

    fun AktivitetskortBestilling.toMuterbareFelter(): AktivitetMuterbareFelter {
        return AktivitetMuterbareFelter(
            tittel = this.aktivitetskort.tittel,
            beskrivelse = this.aktivitetskort.beskrivelse,
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
