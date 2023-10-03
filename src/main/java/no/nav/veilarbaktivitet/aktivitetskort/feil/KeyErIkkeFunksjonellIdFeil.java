package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class KeyErIkkeFunksjonellIdFeil extends AktivitetsKortFunksjonellException {

    public KeyErIkkeFunksjonellIdFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
