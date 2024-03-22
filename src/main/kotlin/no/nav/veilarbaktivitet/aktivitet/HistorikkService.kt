package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
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
                    endretAv = "",
                    tidspunkt = DateUtils.dateToZonedDateTime(aktivitetData.endretDato),
                    beskrivelse = ""
                )
            } else {
                null
            }
        }
        aktivitetId to Historikk(endringer = endringer)
    }.toMap()
}

typealias AktivitetId = Long

data class Historikk(
    val endringer: List<Endring>
)

data class Endring(
    val endretAvType: Innsender,
    val endretAv: String?,
    val tidspunkt: ZonedDateTime,
    val beskrivelse: String,
)
