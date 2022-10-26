package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.arena.model.ArenaId;

public record MeldingContext(
        ArenaId eksternReferanseId,
        String arenaTiltakskode,
        String source,
        AktivitetskortType aktivitetskortType
) {
}
