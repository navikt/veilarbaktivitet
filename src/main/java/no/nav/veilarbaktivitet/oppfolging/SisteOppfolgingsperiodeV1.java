package no.nav.veilarbaktivitet.oppfolging;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Data
public class SisteOppfolgingsperiodeV1 {
    UUID uuid;
    String aktorId;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
}
