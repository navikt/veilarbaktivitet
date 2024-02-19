package no.nav.veilarbaktivitet.oppfolging.periode;

import java.time.ZonedDateTime;
import java.util.UUID;

public record Oppfolgingsperiode(
        String aktorid,
        UUID oppfolgingsperiodeId,
        ZonedDateTime startTid,
        ZonedDateTime sluttTid) {
}
