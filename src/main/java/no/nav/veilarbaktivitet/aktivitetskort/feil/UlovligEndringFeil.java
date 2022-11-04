package no.nav.veilarbaktivitet.aktivitetskort.feil;

public class UlovligEndringFeil extends AktivitetsKortFunksjonellException {
    public UlovligEndringFeil() {
        super(new ErrorMessage("Kan ikke endre aktiviteter som er avbrutt, fullført eller historiske (avsluttet oppfølgingsperiode)"), null);
    }
}
