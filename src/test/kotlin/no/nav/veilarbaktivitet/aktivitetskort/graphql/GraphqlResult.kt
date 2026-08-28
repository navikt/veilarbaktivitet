package no.nav.veilarbaktivitet.aktivitetskort.graphql

class GraphqlResult(
    val data: QueryAktivitetsPerioder?,
    val errors: List<Error>? = null
)

class QueryAktivitetsPerioder(
    val perioder: List<OppfolgingsPeriode>? = emptyList(),
    val aktivitet: AktivitetQueryDto? = null,
    val eier: Eier? = null
)

class Location(val line: String, val column: Int)

class Extension(val classification: String)

class Error (
    val message: String?,
    val locations: List<Location>?,
    val path: List<String>?,
    val extensions: Extension?
)