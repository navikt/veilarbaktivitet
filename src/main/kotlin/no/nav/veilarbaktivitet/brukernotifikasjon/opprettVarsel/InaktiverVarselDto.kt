package no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Produsent

public data class InaktiverVarselDto(
    @JsonProperty("@event_name") val eventName: String,
    val varselId: String,
    val produsent: Produsent,
)

