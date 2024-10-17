package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class InternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: InternVarselHendelseType,
    val namespace: String,
    val appnavn: String,
    val varseltype: String,
    val varselId: UUID,
): VarselHendelse()

enum class InternVarselHendelseType {
    opprettet,
    inaktivert,
    slettet
}
