package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class MessageIdIkkeUnikFeil extends AktivitetsKortFunksjonellException {

    public MessageIdIkkeUnikFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
