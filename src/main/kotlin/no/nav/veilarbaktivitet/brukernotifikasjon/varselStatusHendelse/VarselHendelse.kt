package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.common.json.JsonUtils

enum class VarselEventTypeDto {
    opprettet,
    inaktivert,
    slettet,
    eksternStatusOppdatert // Denne inneholder feltet status som har flere forskjellig statuser
}

/* Alle typer hendelser, inkl ekstern  */
enum class VarselHendelseEventType {
    opprettet,
    inaktivert,
    slettet,
    sendt_ekstern,
    renotifikasjon_ekstern,
    bestilt_ekstern,
    feilet_ekstern
}

fun String.deserialiserVarselHendelse(): VarselHendelse {
    val jsonTree = JsonUtils.getMapper().readTree(this)
    val eventName = jsonTree["@event_name"].asText()
    return when (eventName == VarselEventTypeDto.eksternStatusOppdatert.name) {
        true -> jsonTree.deserialiserEksternVarselHendelse()
        else -> JsonUtils.fromJson(this, InternVarselHendelseDTO::class.java)
    }
}