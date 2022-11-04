package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.arena.model.ArenaId;

public record ArenaMeldingHeaders(
        ArenaId eksternReferanseId,
        String arenaTiltakskode
) { }
