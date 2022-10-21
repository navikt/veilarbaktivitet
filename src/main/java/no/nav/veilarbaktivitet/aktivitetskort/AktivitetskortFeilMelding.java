package no.nav.veilarbaktivitet.aktivitetskort;

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
        super(new ErrorMessage("Kan ikke endre aktiviteter som er avbrutt, fullført eller historiske (avsluttet oppfølgingsperiode)"), null);
    }
}

class DuplikatMeldingFeil extends AktivitetsKortFunksjonellException {
    public DuplikatMeldingFeil() {
        super(new ErrorMessage("Melding allerede handtert, ignorer"), null);
    }
}

class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String errorMessage, Throwable cause) {
        super(new ErrorMessage(errorMessage), cause);
    }
}

record ErrorMessage(
    String value
) {}
record FailingMessage(
    String value
) {}
