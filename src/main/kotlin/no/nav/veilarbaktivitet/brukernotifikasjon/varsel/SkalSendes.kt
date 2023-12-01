package no.nav.veilarbaktivitet.brukernotifikasjon.varsel

import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.person.Person
import java.net.URL

internal data class SkalSendes(
    val brukernotifikasjonLopeNummer: Long,
    val brukernotifikasjonId: String,
    val varselType: VarselType,
    val melding: String,
    val oppfolgingsperiode: String,
    val fnr: Person.Fnr,
    val epostTitel: String,
    val epostBody: String,
    val smsTekst: String,
    val url: URL
)