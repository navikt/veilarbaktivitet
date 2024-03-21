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

        return aktivitetVersjoner.map { (aktivitetId, aktivitetVersjoner) ->
            val sortereteAktivitetVersjoner = aktivitetVersjoner.sortedBy { it.versjon }
            val endringer = sortereteAktivitetVersjoner.mapIndexedNotNull { index, aktivitetData ->
                if (index > 0) {
                    val forrigeAktivitetVersjon = sortereteAktivitetVersjoner[index]
                    lagEndring(forrigeAktivitetVersjon, aktivitetData)
                } else {
                    null
                }
            }
            aktivitetId to Historikk(endringer = endringer)
        }.toMap()
    }

    private fun lagEndring(forrige: AktivitetData, oppdatert: AktivitetData): Endring {
        return Endring(
            endretAvType = oppdatert.endretAvType,
            endretAv = "",
            tidspunkt = DateUtils.dateToZonedDateTime(oppdatert.endretDato),
            beskrivelse = ""
        )
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
    val beskrivelse: String,
)

// TODO: Ligger et enum med samme konstanter i person/Innsender
enum class BrukerType {
    BRUKER,
    ARBEIDSGIVER,
    TILTAKSARRANGOER,
    NAV,
    SYSTEM,
    ARENAIDENT
}