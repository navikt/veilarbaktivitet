package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class UgyldigIdentFeil extends AktivitetsKortFunksjonellException {
    public UgyldigIdentFeil(String errorMessage, Throwable cause) {
        super(new ErrorMessage(errorMessage), cause);
    }
}
