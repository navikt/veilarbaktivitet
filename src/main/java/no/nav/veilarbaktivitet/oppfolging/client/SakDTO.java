package no.nav.veilarbaktivitet.oppfolging.client;

import java.util.UUID;
public record SakDTO(
        UUID oppfolgingsperiodeId,
        Long sakId,
        String fagsaksystem
){}
