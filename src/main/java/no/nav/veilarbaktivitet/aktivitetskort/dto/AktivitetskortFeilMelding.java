package no.nav.veilarbaktivitet.aktivitetskort.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AktivitetskortFeilMelding(
        String key,
        LocalDateTime timestamp,
        String failingMessage,
        String errorMessage
) {
}

