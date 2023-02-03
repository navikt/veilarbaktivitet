package no.nav.veilarbaktivitet.aktivitetskort.feil;

public class AktivitetIkkeFunnetFeil extends AktivitetsKortFunksjonellException {
    public AktivitetIkkeFunnetFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
