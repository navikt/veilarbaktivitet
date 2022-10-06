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
    public AktivitetsKortFunksjonellException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage.value(), cause);
    }
}
class DeserialiseringsFeil extends AktivitetsKortFunksjonellException {

    public DeserialiseringsFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
class UlovligEndringFeil extends AktivitetsKortFunksjonellException {
    public UlovligEndringFeil() {
        super(new ErrorMessage("Ulovlig statusoppdatering"), null);
    }
}

class DuplikatMeldingFeil extends AktivitetsKortFunksjonellException {
    public DuplikatMeldingFeil() {
        super(new ErrorMessage("Melding allerede handtert, ignorer"), null);
    }
}

class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String ugyldigIdent) {
        super(new ErrorMessage(String.format("%s er ikke en gyldig ident", ugyldigIdent)), null);
    }
}

record ErrorMessage(
    String value
) {}
record FailingMessage(
    String value
) {}
