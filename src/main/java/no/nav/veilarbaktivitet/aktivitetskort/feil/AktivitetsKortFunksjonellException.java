package no.nav.veilarbaktivitet.aktivitetskort.feil;

public class AktivitetsKortFunksjonellException extends Exception {
    public AktivitetsKortFunksjonellException(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage.value(), cause);
    }
}
