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

class Error (
    val message: String?,
    val locations: List<Location>?,
    val path: List<String>?,
    val extensions: Extension?
)

class Location(val line: String, val column: Int)

class Extension(val classification: String)

class QueryAktivitetsPerioder(
    val perioder: List<OppfolgingsPeriode>? = emptyList(),
    val aktivitet: AktivitetDTO? = null,
    val eier: Eier? = null
)

class Eier(
    val fnr: String
)

class GraphqlResult(
    val data: QueryAktivitetsPerioder?,
    val errors: List<Error>? = null
)
