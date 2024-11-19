package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId

data class InternVarselHendelse(
    @JsonProperty("@event_name") val eventName: InternVarselHendelseType,
    val namespace: String,
    val appnavn: String,
    val varseltype: Varseltype,
    val varselId: MinSideVarselId,
): VarselHendelse() {
    fun getHendelseType(): VarselHendelseEventType {
        return when (eventName) {
            InternVarselHendelseType.opprettet -> VarselHendelseEventType.opprettet
            InternVarselHendelseType.inaktivert -> VarselHendelseEventType.inaktivert
            InternVarselHendelseType.slettet -> VarselHendelseEventType.slettet
        }
    }
}

enum class InternVarselHendelseType {
    opprettet,
    inaktivert,
    slettet
}
