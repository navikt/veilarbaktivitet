package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt

import no.nav.veilarbaktivitet.person.Person
import java.util.*

data class SkalAvluttes(
    val brukernotifikasjonId: String,
    val fnr: Person.Fnr,
    val oppfolgingsperiode: UUID
)