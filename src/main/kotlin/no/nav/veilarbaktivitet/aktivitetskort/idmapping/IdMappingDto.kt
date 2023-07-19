package no.nav.veilarbaktivitet.aktivitetskort.idmapping

import no.nav.veilarbaktivitet.arena.model.ArenaId
import java.util.*

data class IdMappingDto(val arenaId: ArenaId, val aktivitetId: Long, val funksjonellId: UUID) {
    companion object {
        fun map(idMapping: IdMapping): IdMappingDto {
            return IdMappingDto(idMapping.arenaId, idMapping.aktivitetId, idMapping.funksjonellId)
        }
    }
}
