package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class DeserialiseringsFeil extends AktivitetsKortFunksjonellException {

    public DeserialiseringsFeil(ErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
