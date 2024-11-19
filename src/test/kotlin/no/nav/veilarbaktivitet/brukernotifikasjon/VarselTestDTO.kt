package no.nav.veilarbaktivitet.brukernotifikasjon

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselKanal
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus
import java.util.*

sealed class TestVarselHendelseDTO() {}

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
): TestVarselHendelseDTO()

data class InternVarselHendelseDTO(
    @JsonProperty("@event_name") val eventName: String,
    val namespace: String,
    val appnavn: String,
    val varseltype: Varseltype,
    val varselId: UUID,
): TestVarselHendelseDTO()