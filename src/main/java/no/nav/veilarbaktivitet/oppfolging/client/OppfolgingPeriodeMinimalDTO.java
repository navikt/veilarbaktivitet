package no.nav.veilarbaktivitet.oppfolging.client;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class OppfolgingPeriodeMinimalDTO {
    private UUID uuid;
    private ZonedDateTime startDato;
    private ZonedDateTime sluttDato;
}
