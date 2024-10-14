package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

sealed class VarselHendelse

data class EksternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varseltype: String,
    val varselId: UUID,
    val status: EksternVarselStatus,
    val renotifikasjon: Boolean? = null,
    val feilmelding: String? = null,
    val kanal: EksternVarselKanal? = null
): VarselHendelse()

enum class EksternVarselKanal {
    SMS,
    EPOST
}

enum class EksternVarselStatus {
    bestilt,
    sendt,
    feilet
}
