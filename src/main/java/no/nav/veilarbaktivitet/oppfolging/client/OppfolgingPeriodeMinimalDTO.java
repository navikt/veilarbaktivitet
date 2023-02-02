package no.nav.veilarbaktivitet.oppfolging.client;


import java.time.ZonedDateTime;
import java.util.UUID;

public record OppfolgingPeriodeMinimalDTO(
    UUID uuid,
    ZonedDateTime startDato,
    ZonedDateTime sluttDato)
{
}
