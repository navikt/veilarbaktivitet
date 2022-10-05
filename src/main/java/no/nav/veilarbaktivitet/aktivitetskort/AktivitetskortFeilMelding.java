package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;

import java.time.LocalDateTime;
import java.lang.String;
import java.util.UUID;

@Builder
public record AktivitetskortFeilMelding(
        String key,
        LocalDateTime timestamp,
        String failingMessage,
        String errorMessage
) {
}

class AktivitetsKortFunksjonellException extends Exception {
    public final String key;
    public AktivitetsKortFunksjonellException(String key, ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage.value(), cause);
        this.key = key;
    }
}
class DeserialiseringsFeil extends AktivitetsKortFunksjonellException {

    public DeserialiseringsFeil(String key, ErrorMessage errorMessage, Throwable cause) {
        super(key, errorMessage, cause);
    }
}
class UlovligEndringFeil extends AktivitetsKortFunksjonellException {
    public UlovligEndringFeil(String key) {
        super(key, new ErrorMessage("Ulovlig statusoppdatering"), null);
    }
}

class DuplikatMeldingFeil extends AktivitetsKortFunksjonellException {
    public DuplikatMeldingFeil(UUID key) {
        super(key.toString(), new ErrorMessage("Melding allerede handtert, ignorer"), null);
    }
}

class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String ugyldigIdent, ErrorMessage errorMessage, Throwable cause) {
        super(String.format("%s er ikke en gyldig ident", ugyldigIdent), errorMessage, cause);
    }
}

record ErrorMessage(
    String value
) {}
record FailingMessage(
    String value
) {}
