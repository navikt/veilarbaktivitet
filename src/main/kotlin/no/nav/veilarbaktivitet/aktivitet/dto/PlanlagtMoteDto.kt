package no.nav.veilarbaktivitet.aktivitet.dto

import java.time.ZonedDateTime

data class PlanlagtMoteDto(
    val id: String,
    val fraDato: ZonedDateTime,
)
