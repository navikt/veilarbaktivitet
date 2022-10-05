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
    public final FailingMessage failingMessage;
    public final String key;
    public AktivitetsKortFunksjonellException(String key, ErrorMessage errorMessage, FailingMessage failingMessage, Throwable cause) {
        super(errorMessage.value() , cause);
        this.failingMessage = failingMessage;
        this.key = key;
    }
}
class DeserialiseringsFeil extends AktivitetsKortFunksjonellException {

    public DeserialiseringsFeil(String key, ErrorMessage errorMessage, FailingMessage failingMessage, Throwable cause) {
        super(key, errorMessage, failingMessage, cause);
    }
}
class UlovligEndringFeil extends AktivitetsKortFunksjonellException {
    public UlovligEndringFeil(String key) {
        super(key, new ErrorMessage("Ulovlig statusoppdatering"), null, null);
    }
}

class DuplikatMeldingFeil extends AktivitetsKortFunksjonellException {
    public DuplikatMeldingFeil(UUID key, FailingMessage failingMessage) {
        super(key.toString(), new ErrorMessage("Melding allerede handtert, ignorer"), failingMessage, null);
    }
}

class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String ugyldigIdent, ErrorMessage errorMessage, FailingMessage failingMessage, Throwable cause) {
        super(String.format("%s er ikke en gyldig ident", ugyldigIdent), errorMessage, failingMessage, cause);
    }
}

record ErrorMessage(
    String value
) {}
record FailingMessage(
    String value
) {}
