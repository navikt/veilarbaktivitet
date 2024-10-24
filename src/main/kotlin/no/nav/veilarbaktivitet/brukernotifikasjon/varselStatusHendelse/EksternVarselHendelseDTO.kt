package no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import java.util.*

sealed class VarselHendelse

/* Kun brukt i tester foreløpig */
data class EksternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varseltype: Varseltype,
    val varselId: UUID,
    val status: EksternVarselStatus,
    val renotifikasjon: Boolean? = null, // Også kalt "revarsling"
    val feilmelding: String? = null,
    val kanal: EksternVarselKanal? = null
)
object VarselFraAnnenApp: VarselHendelse()
sealed class EksternVarsling(
    val varselId: MinSideVarselId,
    val hendelseType: VarselHendelseEventType,
    val varseltype: Varseltype
): VarselHendelse()

class Renotifikasjon(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarsling(varselId, VarselHendelseEventType.renotifikasjon_ekstern, varseltype)
class Sendt(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
    val kanal: EksternVarselKanal
): EksternVarsling(varselId, VarselHendelseEventType.sendt_ekstern, varseltype)
class Bestilt(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
): EksternVarsling(varselId, VarselHendelseEventType.bestilt_ekstern, varseltype)
class Feilet(
    varseltype: Varseltype,
    varselId: MinSideVarselId,
    val feilmelding: String
): EksternVarsling(varselId, VarselHendelseEventType.feilet_ekstern, varseltype)

fun JsonNode.deserialiserEksternVarselHendelse(): EksternVarsling {
    val eksternStatus = EksternVarselStatus.valueOf(this["status"].asText())
    val varselId = MinSideVarselId(UUID.fromString(this["varselId"].asText()))
    val varseltype = Varseltype.valueOf(this["varseltype"].asText().replaceFirstChar { it.titlecase()})
    return when (eksternStatus) {
        EksternVarselStatus.sendt -> {
            val kanal = EksternVarselKanal.valueOf(this["kanal"].asText())
            when (this["renotifikasjon"].asBoolean()) {
                true -> Renotifikasjon(varseltype, varselId)
                else -> {
                    Sendt(varseltype, varselId, kanal)
                }
            }
        }
        EksternVarselStatus.bestilt -> {
            Bestilt(
                varseltype,
                varselId
            )
        }
        EksternVarselStatus.feilet -> {
            return Feilet(
                varseltype,
                varselId,
                this["feilmelding"].asText()
            )
        }
    }
}

enum class EksternVarselKanal {
    SMS,
    EPOST
}

enum class EksternVarselStatus {
    bestilt,
    sendt,
    feilet
}
