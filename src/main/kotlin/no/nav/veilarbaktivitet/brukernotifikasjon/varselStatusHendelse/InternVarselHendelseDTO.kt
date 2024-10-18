package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId

data class InternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: InternVarselHendelseType,
    val namespace: String,
    val appnavn: String,
    val varseltype: VarselEventTypeDto,
    val varselId: MinSideVarselId,
): VarselHendelse()

enum class InternVarselHendelseType {
    opprettet,
    inaktivert,
    slettet
}
