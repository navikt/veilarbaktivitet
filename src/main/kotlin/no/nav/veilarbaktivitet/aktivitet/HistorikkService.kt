package no.nav.veilarbaktivitet.aktivitet

import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class HistorikkService(
    private val aktivitetDAO: AktivitetDAO
) {

    fun hentHistorikk(aktivitetIder: List<Long>): Map<Long, Historikk> {
        aktivitetDAO.hentAktivitetVersjoner(aktivitetIder)
    }
}


data class Historikk(
    val endringer: List<Endring>
)

data class Endring(
    val endretAvType: BrukerType,
    val endretAv: String?,
    val tidspunkt: ZonedDateTime,
    val beskrivelse: String,

)
enum class BrukerType {
    BRUKER,
    ARBEIDSGIVER,
    TILTAKSARRANGOER,
    NAV,
    SYSTEM,
    ARENAIDENT
}