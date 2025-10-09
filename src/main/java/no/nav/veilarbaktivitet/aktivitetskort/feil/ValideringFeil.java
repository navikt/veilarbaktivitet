package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class ValideringFeil extends AktivitetsKortFunksjonellException {

    public ValideringFeil(String melding) {
        super(new ErrorMessage(melding), null);
    }
}
