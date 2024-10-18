package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import java.util.*

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
    val appNavn = jsonTree["appnavn"].asText()
    if (appNavn != "veilarbaktivitet") return VarselFraAnnenApp
    return when (eventName == VarselEventTypeDto.eksternStatusOppdatert.name) {
        true -> jsonTree.deserialiserEksternVarselHendelse()
        else -> {
            return InternVarselHendelseDTO(
                namespace = jsonTree["namespace"].asText(),
                varseltype = VarselEventTypeDto.valueOf(jsonTree["varseltype"].asText()),
                appnavn = appNavn,
                varselId = MinSideVarselId(UUID.fromString(jsonTree["varselId"].asText())),
                eventName = InternVarselHendelseType.valueOf(eventName)
            )
        }
    }
}