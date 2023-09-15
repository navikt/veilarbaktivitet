package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class UlovligEndringFeil extends AktivitetsKortFunksjonellException {

    public UlovligEndringFeil(String melding) {
        super(new ErrorMessage(melding), null);
    }
}
