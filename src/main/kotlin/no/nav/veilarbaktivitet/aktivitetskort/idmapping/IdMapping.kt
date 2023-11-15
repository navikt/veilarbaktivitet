package no.nav.veilarbaktivitet.aktivitetskort.idmapping

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaId
import java.util.*

data class IdMapping(
    public val arenaId: ArenaId,
    public val aktivitetId: Long,
    public val funksjonellId: UUID,
    public val status: AktivitetStatus
)
