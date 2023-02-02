package no.nav.veilarbaktivitet.aktivitetskort.feil;

public class MessageIdIkkeUnikFeil extends AktivitetsKortFunksjonellException {

    public MessageIdIkkeUnikFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
