package no.nav.veilarbaktivitet.aktivitetskort.feil;

public final class ManglerOppfolgingsperiodeFeil extends AktivitetsKortFunksjonellException {
    public ManglerOppfolgingsperiodeFeil() {
        super(new ErrorMessage("Finner ingen passende oppfølgingsperiode for aktivitetskortet."), null);
    }
}
