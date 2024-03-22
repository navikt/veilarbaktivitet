package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.util.DateUtils
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

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
                    beskrivelseForVeileder = hentEndringstekst(aktivitetData.transaksjonsType, endretAvTekstTilVeileder(aktivitetData.endretAvType, aktivitetData.endretAv), sorterteAktivitetVersjoner.getOrNull(index-1), aktivitetData),
                    beskrivelseForBruker = hentEndringstekst(aktivitetData.transaksjonsType, endretAvTekstTilBruker(aktivitetData.endretAvType, aktivitetData.endretAv), sorterteAktivitetVersjoner.getOrNull(index-1), aktivitetData)
                )
        }
        val endringerSortertMedNyesteEndringFørst = endringer.sortedByDescending { it.tidspunkt }
        aktivitetId to Historikk(endringer = endringerSortertMedNyesteEndringFørst)
    }.toMap()
}

private fun endretAvTekstTilVeileder(innsender: Innsender, endretAv: String?) = when(innsender) {
    Innsender.BRUKER -> "Bruker"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver${endretAv?.let { " $it" } ?: ""}"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør${endretAv?.let { " $it" } ?: ""}"
    Innsender.NAV, Innsender.ARENAIDENT -> endretAv?.let { "$endretAv" } ?: "NAV"
    Innsender.SYSTEM -> "NAV"
}

private fun endretAvTekstTilBruker(innsender: Innsender, endretAv: String?) = when(innsender) {
    Innsender.BRUKER -> "Du"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør"
    Innsender.NAV, Innsender.ARENAIDENT, Innsender.SYSTEM -> "NAV"
}

private fun hentEndringstekst(transaksjonsType: AktivitetTransaksjonsType, endretAvTekst: String, forrigeVersjon: AktivitetData?, oppdatertVersjon: AktivitetData): String {
    return when(transaksjonsType) {
        AktivitetTransaksjonsType.OPPRETTET -> ""
        AktivitetTransaksjonsType.STATUS_ENDRET -> "$endretAvTekst flyttet aktiviteten fra ${forrigeVersjon?.status} til ${oppdatertVersjon.status}"
        AktivitetTransaksjonsType.DETALJER_ENDRET -> ""
        AktivitetTransaksjonsType.AVTALT -> ""
        AktivitetTransaksjonsType.AVTALT_DATO_ENDRET -> ""
        AktivitetTransaksjonsType.ETIKETT_ENDRET -> ""
        AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET -> "$endretAvTekst endret tid eller sted for møtet"
        AktivitetTransaksjonsType.REFERAT_OPPRETTET -> ""
        AktivitetTransaksjonsType.REFERAT_ENDRET -> ""
        AktivitetTransaksjonsType.REFERAT_PUBLISERT -> ""
        AktivitetTransaksjonsType.BLE_HISTORISK -> ""
        AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST -> ""
        AktivitetTransaksjonsType.DEL_CV_SVART -> ""
        AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET -> ""
        AktivitetTransaksjonsType.IKKE_FATT_JOBBEN -> ""
        AktivitetTransaksjonsType.FATT_JOBBEN -> ""
    }
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
