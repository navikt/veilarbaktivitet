package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import java.time.ZonedDateTime
import java.util.*

class OppfolgingsPeriode(
    val id: UUID?,
    val aktiviteter: List<AktivitetDTO> = emptyList(),
    val start: ZonedDateTime?,
    val slutt: ZonedDateTime?,
)

class Eier(
    val fnr: String
)
