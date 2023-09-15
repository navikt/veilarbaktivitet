package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class DuplikatMeldingFeil extends AktivitetsKortFunksjonellException {
    public DuplikatMeldingFeil() {
        super(new ErrorMessage("Melding allerede handtert, ignorer"), null);
    }
}
