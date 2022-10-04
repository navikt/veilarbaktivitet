package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AktivitetskortFeilMelding(
        UUID messageId,
        UUID aktivitetId,
        String feilmelding,
        String payload,
        String errorMessage
) {
}

class AktivitetsKortFunksjonellException extends Exception {
    public AktivitetsKortFunksjonellException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
    public AktivitetsKortFunksjonellException(String errorMessage) {
        super(errorMessage);
    }
}
class DeserialiseringsFeil extends AktivitetsKortFunksjonellException {
    public DeserialiseringsFeil(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
class UlovligStatusOvergangsFeil extends AktivitetsKortFunksjonellException {
    public UlovligStatusOvergangsFeil(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}

class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String ugyldigIdent, Throwable cause) {
        super(String.format("%s er ikke en gyldig ident", ugyldigIdent), cause);
    }
}
