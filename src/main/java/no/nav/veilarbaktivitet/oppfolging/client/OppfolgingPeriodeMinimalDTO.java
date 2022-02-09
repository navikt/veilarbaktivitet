package no.nav.veilarbaktivitet.oppfolging.client;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@With
public class OppfolgingPeriodeMinimalDTO {
    private UUID uuid;
    private ZonedDateTime startDato;
    private ZonedDateTime sluttDato;
}
