package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.common.json.JsonUtils

enum class EksternVarselEventType {
    opprettet,
    inaktivert,
    slettet,
    eksternStatusOppdatert
}

class VarselHendelseUtils {
    companion object {

    }
}

fun String.deserialiserVarselHendelse(): VarselHendelse {
    val jsonTree = JsonUtils.getMapper().readTree(this)
    val eventName = jsonTree["@event_name"].asText()
    return when (eventName == EksternVarselEventType.eksternStatusOppdatert.name) {
        true -> JsonUtils.fromJson(this, EksternVarselHendelseDTO::class.java)
        else -> JsonUtils.fromJson(this, InternVarselHendelseDTO::class.java)
    }
}