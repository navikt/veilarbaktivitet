package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.person.Innsender
import java.time.ZonedDateTime

data class SporingsData(
    val endretAv: String,
    val endretAvType: Innsender,
    val endretDato: ZonedDateTime,
)