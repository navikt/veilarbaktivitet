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

        return lagHistorikk(aktivitetVersjoner)
    }

}

fun lagHistorikk(aktivitetVersjoner: Map<AktivitetId, List<AktivitetData>>): Map<AktivitetId, Historikk> {
    return aktivitetVersjoner.map { (aktivitetId, aktivitetVersjoner) ->
        val sortereteAktivitetVersjoner = aktivitetVersjoner.sortedBy { it.versjon }
        val endringer = sortereteAktivitetVersjoner.mapIndexedNotNull { index, aktivitetData ->
            if (index > 0) {
                val forrigeAktivitetVersjon = sortereteAktivitetVersjoner[index]
                Endring(
                    endretAvType = aktivitetData.endretAvType,
                    endretAv = aktivitetData.endretAv,
                    tidspunkt = DateUtils.dateToZonedDateTime(aktivitetData.endretDato),
                    beskrivelseForVeileder = hentEndringstekst(aktivitetData.transaksjonsType, aktivitetData.endretAvType, aktivitetData.endretAv),
                    beskrivelseForBruker = hentEndringstekst(aktivitetData.transaksjonsType, aktivitetData.endretAvType, aktivitetData.endretAv)
                )
            } else {
                null
            }
        }
        aktivitetId to Historikk(endringer = endringer)
    }.toMap()
}

private fun hentEndringstekst(transaksjonsType: AktivitetTransaksjonsType, innsender: Innsender, endretAv: String?): String {
    val endretAvTekstTilVeileder = when(innsender) {
        Innsender.BRUKER -> "Bruker"
        Innsender.ARBEIDSGIVER -> "Arbeidsgiver${endretAv?.let { " $it" } ?: ""}"
        Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør${endretAv?.let { " $it" } ?: ""}"
        Innsender.NAV -> endretAv?.let { "$endretAv" } ?: "NAV"
        Innsender.SYSTEM -> "NAV"
        Innsender.ARENAIDENT -> endretAv?.let { "$endretAv" } ?: "NAV"
    }

    val endretAvTekstTilBruker = ""

    return when(transaksjonsType) {
        AktivitetTransaksjonsType.OPPRETTET -> ""
        AktivitetTransaksjonsType.STATUS_ENDRET -> ""
        AktivitetTransaksjonsType.DETALJER_ENDRET -> ""
        AktivitetTransaksjonsType.AVTALT -> ""
        AktivitetTransaksjonsType.AVTALT_DATO_ENDRET -> ""
        AktivitetTransaksjonsType.ETIKETT_ENDRET -> ""
        AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET -> "$endretAvTekstTilVeileder endret tid eller sted for møtet"
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
