package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

import lombok.Builder;
import no.nav.veilarbaktivitet.aktivitetskort.dto.ErrorType;

import java.time.LocalDateTime;

@Builder
public record AktivitetskortFeilMelding(
        String key,
        LocalDateTime timestamp,
        String failingMessage,
        String errorMessage,
        MessageSource source,
        ErrorType errorType
) {
}

