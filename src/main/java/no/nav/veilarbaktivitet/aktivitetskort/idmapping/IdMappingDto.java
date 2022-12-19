package no.nav.veilarbaktivitet.aktivitetskort.idmapping;


import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.util.UUID;

public record IdMappingDto(
    ArenaId areanaId,
    Long aktivitetId,
    UUID funksjonellId
) {

    public static IdMappingDto map(IdMapping idMapping) {
        return new IdMappingDto(idMapping.areanaId(), idMapping.aktivitetId(), idMapping.funksjonellId());
    }
}
