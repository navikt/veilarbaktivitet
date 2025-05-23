package no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel

import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.person.Person
import java.net.URI
import java.net.URL
import java.util.*

data class UtgåendeVarsel (
    val varselId: MinSideVarselId,
    val foedselsnummer: Person.Fnr,
    val melding: String,
    val oppfolgingsperiode: UUID,
    val type: VarselType,
    val status: VarselStatus,
    val url: URL,
    val epostTitel: String?,
    val epostBody: String?,
    val smsTekst: String?
)

fun createAktivitetLink(aktivitetsplanBasepath: String, aktivitetId: String): URL {
    return URI.create("$aktivitetsplanBasepath/aktivitet/vis/$aktivitetId").toURL()
}