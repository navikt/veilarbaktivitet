package no.nav.veilarbaktivitet.oppfolging.client;

import java.util.UUID;
public record SakDTO(
        UUID oppfolgingsperiodeId,
        Long sakId,
        String fagsaksystem,
        String tema // TOOD: Ikke bruk denne, vi tror en sak kan ha journalposter på flere forskjellige temaer
){}
