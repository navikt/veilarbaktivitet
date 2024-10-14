package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusEndring

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class EksternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varselType: String,
    val varselId: UUID,
    val status: EksternVarselStatus
)

enum class EksternVarselStatus {
    bestilt,
    sendt,
    feilet
}
