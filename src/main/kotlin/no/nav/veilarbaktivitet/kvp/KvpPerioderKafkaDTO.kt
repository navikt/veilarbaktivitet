package no.nav.veilarbaktivitet.kvp

import java.time.ZonedDateTime

typealias KvpPerioderKafkaDTO = KvpPeriode

data class KvpPeriode(
    val event: KvpPeriodeEventType,
    val aktorId: String,
    val enhetId: String?,
    val startet: KvpStartet,
    val avsluttet: KvpAvsluttet?
)

enum class KvpPeriodeEventType {
    STARTET,
    AVSLUTTET
}

data class KvpStartet(
    var opprettetAv: String,
    var opprettetDato: ZonedDateTime,
    var opprettetBegrunnelse: String?
)

data class KvpAvsluttet(
    val avsluttetAv: String,
    val avsluttetDato: ZonedDateTime,
    val avsluttetBegrunnelse: String?
)
