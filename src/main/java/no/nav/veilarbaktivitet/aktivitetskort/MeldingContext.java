package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.arena.model.ArenaId;

public record MeldingContext(
        ArenaId eksternReferanseId,
        String arenaTiltakskode,
        String source,
        AktivitetskortType aktivitetskortType
) {
    public static final String HEADER_EKSTERN_REFERANSE_ID = "eksternReferanseId";
    public static final String HEADER_EKSTERN_ARENA_TILTAKSKODE = "arenaTiltakskode";
}
