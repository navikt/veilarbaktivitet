package no.nav.veilarbaktivitet.oppfolging.periode;

import java.time.ZonedDateTime;
import java.util.UUID;

public record Oppfolgingsperiode(String aktorid, UUID oppfolgingsperiode, ZonedDateTime startTid,
                                 ZonedDateTime sluttTid) {
}
