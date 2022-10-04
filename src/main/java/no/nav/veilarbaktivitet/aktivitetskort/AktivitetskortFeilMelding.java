package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AktivitetskortFeilMelding(
        UUID messageId,
        UUID aktivitetId,
        String feilmelding,
        String payload
) {
}
