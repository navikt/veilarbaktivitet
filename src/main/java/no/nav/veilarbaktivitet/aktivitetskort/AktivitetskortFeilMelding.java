package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.IngenGjeldendePeriodeException;

import java.time.LocalDateTime;

@Builder
public record AktivitetskortFeilMelding(
        String key,
        LocalDateTime timestamp,
        String failingMessage,
        String errorMessage
) {
}

