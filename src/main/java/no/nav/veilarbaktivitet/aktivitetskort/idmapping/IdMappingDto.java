package no.nav.veilarbaktivitet.aktivitetskort.idmapping;


import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.util.UUID;

public record IdMappingDto(
    ArenaId arenaId,
    Long aktivitetId,
    UUID funksjonellId
) {

    public static IdMappingDto map(IdMapping idMapping) {
        return new IdMappingDto(idMapping.arenaId(), idMapping.aktivitetId(), idMapping.funksjonellId());
    }
}
