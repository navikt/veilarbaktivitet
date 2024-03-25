package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.arkivering.etiketter.getArkivEtiketter
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class HistorikkService(
    private val aktivitetDAO: AktivitetDAO
) {

    fun hentHistorikk(aktivitetIder: List<AktivitetId>): Map<AktivitetId, Historikk> {
        val aktivitetVersjoner: Map<AktivitetId, List<AktivitetData>> = aktivitetDAO.hentAktivitetVersjoner(aktivitetIder)

        return lagHistorikkForAktiviteter(aktivitetVersjoner)
    }

}

fun lagHistorikkForAktiviteter(aktivitetVersjoner: Map<AktivitetId, List<AktivitetData>>): Map<AktivitetId, Historikk> {
    return aktivitetVersjoner.map { (aktivitetId, aktivitetVersjoner) ->
        val sorterteAktivitetVersjoner = aktivitetVersjoner.sortedBy { it.versjon }
        val endringer = sorterteAktivitetVersjoner.mapIndexed { index, aktivitetData ->
                Endring(
                    endretAvType = aktivitetData.endretAvType,
                    endretAv = aktivitetData.endretAv,
                    tidspunkt = DateUtils.dateToZonedDateTime(aktivitetData.endretDato),
                    beskrivelseForVeileder = hentEndringstekst(sorterteAktivitetVersjoner.getOrNull(index-1), aktivitetData, tilBruker = false),
                    beskrivelseForBruker = hentEndringstekst(sorterteAktivitetVersjoner.getOrNull(index-1), aktivitetData, tilBruker = true)
                )
        }
        val endringerSortertMedNyesteEndringFørst = endringer.sortedByDescending { it.tidspunkt }
        aktivitetId to Historikk(endringer = endringerSortertMedNyesteEndringFørst)
    }.toMap()
}

private fun hentEndringstekst(forrigeVersjon: AktivitetData?, oppdatertVersjon: AktivitetData, tilBruker: Boolean): String {
    val endretAvTekst = if (tilBruker) {
        endretAvTekstTilBruker(oppdatertVersjon.endretAvType)
    } else {
        endretAvTekstTilVeileder(oppdatertVersjon.endretAvType, oppdatertVersjon.endretAv)
    }

    return when(oppdatertVersjon.transaksjonsType) {
        AktivitetTransaksjonsType.OPPRETTET -> {
            if (oppdatertVersjon.isAvtalt)
                "$endretAvTekst opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\""
            else
                "$endretAvTekst opprettet aktiviteten"
        }
        AktivitetTransaksjonsType.STATUS_ENDRET -> "$endretAvTekst flyttet aktiviteten fra ${forrigeVersjon?.status} til ${oppdatertVersjon.status}"
        AktivitetTransaksjonsType.DETALJER_ENDRET -> "$endretAvTekst endret detaljer på aktiviteten"
        AktivitetTransaksjonsType.AVTALT -> {
            if (forrigeVersjon?.isAvtalt ?: false)
                "$endretAvTekst sendte forhåndsorientering"
             else
                "$endretAvTekst merket aktiviteten som \"Avtalt med NAV\""
        }
        AktivitetTransaksjonsType.AVTALT_DATO_ENDRET -> "$endretAvTekst endret til dato på aktiviteten fra ${if(forrigeVersjon?.tilDato !== null) norskDato(forrigeVersjon.tilDato) else "ingen dato"} til ${norskDato(oppdatertVersjon.tilDato)}"
        AktivitetTransaksjonsType.ETIKETT_ENDRET -> ""
        AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET -> "$endretAvTekst endret tid eller sted for møtet"
        AktivitetTransaksjonsType.REFERAT_OPPRETTET -> "$endretAvTekst opprettet referat"
        AktivitetTransaksjonsType.REFERAT_ENDRET -> "$endretAvTekst endret referatet"
        AktivitetTransaksjonsType.REFERAT_PUBLISERT -> "$endretAvTekst delte referatet"
        AktivitetTransaksjonsType.BLE_HISTORISK -> "Aktiviteten ble automatisk arkivert"
        AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST -> {
            val sittEllerDitt = if (tilBruker) "ditt" else "sitt"
            "$endretAvTekst bekreftet å ha lest informasjon om ansvaret $sittEllerDitt"
        }
        AktivitetTransaksjonsType.DEL_CV_SVART -> {
            val svar = if (oppdatertVersjon.stillingFraNavData?.cvKanDelesData?.kanDeles ?: false) "Ja" else "Nei"
            "$endretAvTekst svarte '$svar' på spørsmålet \"Er du interessert i denne stillingen?\""
        }
        AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET -> {
            val status = if(oppdatertVersjon.stillingFraNavData?.soknadsstatus !== null) oppdatertVersjon.stillingFraNavData.soknadsstatus.text else "Ingen"
            "$endretAvTekst endret tilstand til $status"
        }
        AktivitetTransaksjonsType.IKKE_FATT_JOBBEN, AktivitetTransaksjonsType.FATT_JOBBEN -> "$endretAvTekst avsluttet aktiviteten fordi kandidaten har ${oppdatertVersjon.stillingFraNavData.soknadsstatus.text}"
    }
}

private fun endretAvTekstTilVeileder(innsender: Innsender, endretAv: String?) = when(innsender) {
    Innsender.BRUKER -> "Bruker"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver${endretAv?.let { " $it" } ?: ""}"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør${endretAv?.let { " $it" } ?: ""}"
    Innsender.NAV, Innsender.ARENAIDENT -> endretAv?.let { "$endretAv" } ?: "NAV"
    Innsender.SYSTEM -> "NAV"
}

private fun endretAvTekstTilBruker(innsender: Innsender) = when(innsender) {
    Innsender.BRUKER -> "Du"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør"
    Innsender.NAV, Innsender.ARENAIDENT, Innsender.SYSTEM -> "NAV"
}

typealias AktivitetId = Long

data class Historikk(
    val endringer: List<Endring>
)

data class Endring(
    val endretAvType: Innsender,
    val endretAv: String?,
    val tidspunkt: ZonedDateTime,
    val beskrivelseForVeileder: String,
    val beskrivelseForBruker: String,
)
