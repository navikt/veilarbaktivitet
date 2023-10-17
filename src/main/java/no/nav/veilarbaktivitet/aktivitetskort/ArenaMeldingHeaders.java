package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record ArenaMeldingHeaders(
        ArenaId eksternReferanseId,
        String arenaTiltakskode,
        UUID oppfolgingsperiode,
        ZonedDateTime oppfolgingsperiodeSlutt
) { }
