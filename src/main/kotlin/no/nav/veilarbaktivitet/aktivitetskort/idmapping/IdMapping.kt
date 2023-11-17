package no.nav.veilarbaktivitet.aktivitetskort.idmapping

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaId
import java.time.LocalDateTime
import java.util.*

data class IdMapping(
    val arenaId: ArenaId,
    val aktivitetId: Long,
    val funksjonellId: UUID,
)

data class IdMappingWithAktivitetStatus (
    val arenaId: ArenaId,
    val aktivitetId: Long,
    val funksjonellId: UUID,
    val status: AktivitetStatus,
    val historiskDato: LocalDateTime?
)