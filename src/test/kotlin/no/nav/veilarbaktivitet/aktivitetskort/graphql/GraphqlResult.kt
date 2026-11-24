package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.veilarbaktivitet.aktivitet.domain.PlanlagtMote

class GraphqlResult(
    val data: QueryAktivitetsPerioder?,
    val errors: List<Error>? = null
)

class QueryAktivitetsPerioder(
    val perioder: List<OppfolgingsPeriode>? = emptyList(),
    val aktivitet: AktivitetQueryDto? = null,
    val eier: Eier? = null,
    val brukersNavn: BrukerNavn? = null,
    val nestePlanlagteMote: PlanlagtMote? = null,
)

class Location(val line: String, val column: Int)

class Extension(val classification: String)

class Error (
    val message: String?,
    val locations: List<Location>?,
    val path: List<String>?,
    val extensions: Extension?
)