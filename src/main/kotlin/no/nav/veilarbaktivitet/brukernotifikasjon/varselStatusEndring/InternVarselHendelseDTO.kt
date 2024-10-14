package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusEndring

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class InternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varselType: String,
    val varselId: UUID,
)
