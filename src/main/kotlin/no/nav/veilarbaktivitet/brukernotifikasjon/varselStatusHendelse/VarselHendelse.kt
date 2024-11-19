package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import no.nav.common.json.JsonUtils
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import java.util.*

/* Alle typer hendelser, inkl ekstern  */
enum class VarselHendelseEventType {
    opprettet,
    inaktivert,
    slettet,
    sendt_ekstern,
    renotifikasjon_ekstern,
    bestilt_ekstern,
    feilet_ekstern,
    venter_ekstern,
    kansellert_ekstern,
    ferdigstil_ektsern,
}

const val EksternStatusOppdatertEventName = "eksternStatusOppdatert"

fun String.deserialiserVarselHendelse(): VarselHendelse {
    val jsonTree = JsonUtils.getMapper().readTree(this)
    val eventName = jsonTree["@event_name"].asText()
    val appNavn = jsonTree["appnavn"].asText()
    if (appNavn != "veilarbaktivitet") return VarselFraAnnenApp
    val varseltype = Varseltype.valueOf(jsonTree["varseltype"].asText().replaceFirstChar { it.titlecase()})
    return when (eventName == EksternStatusOppdatertEventName) {
        true -> jsonTree.deserialiserEksternVarselHendelse()
        else -> {
            return InternVarselHendelse(
                namespace = jsonTree["namespace"].asText(),
                varseltype = varseltype,
                appnavn = appNavn,
                varselId = MinSideVarselId(UUID.fromString(jsonTree["varselId"].asText())),
                eventName = InternVarselHendelseType.valueOf(eventName)
            )
        }
    }
}