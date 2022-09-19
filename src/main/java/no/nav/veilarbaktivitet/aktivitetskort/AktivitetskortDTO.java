package no.nav.veilarbaktivitet.aktivitetskort;

import java.time.LocalDateTime;
import java.util.UUID;

public record AktivitetskortDTO(
        UUID id,
        String utsender, // "ARENA_TILTAK_AKTIVITET_ACL",
        LocalDateTime sendt,
        ActionType actionType,
        Object payload // f.eks UpsertTiltakAktivitetV1/TiltakAktivitet
) {
}
