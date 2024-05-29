package no.nav.veilarbaktivitet.oppfolging

import java.time.ZonedDateTime
import java.util.*

data class OppfolgingPeriodeMinimalDTO(
    val uuid: UUID,
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?
)
