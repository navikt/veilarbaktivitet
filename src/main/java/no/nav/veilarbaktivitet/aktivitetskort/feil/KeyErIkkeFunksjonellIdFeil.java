package no.nav.veilarbaktivitet.aktivitetskort.feil;

public class KeyErIkkeFunksjonellIdFeil extends AktivitetsKortFunksjonellException {

    public KeyErIkkeFunksjonellIdFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
