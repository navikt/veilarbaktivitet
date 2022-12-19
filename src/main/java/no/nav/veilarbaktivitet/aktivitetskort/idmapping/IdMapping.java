package no.nav.veilarbaktivitet.aktivitetskort.idmapping;


import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.util.UUID;

public record IdMapping(
    ArenaId aranaId,
    Long aktivitetId,
    UUID funksjonellId
) {}
