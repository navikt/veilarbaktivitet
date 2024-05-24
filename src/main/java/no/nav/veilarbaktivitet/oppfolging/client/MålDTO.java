package no.nav.veilarbaktivitet.oppfolging.client;

import java.time.ZonedDateTime;

public record MålDTO(
        String mal,
        String endretAv,
        ZonedDateTime dato
) {
}