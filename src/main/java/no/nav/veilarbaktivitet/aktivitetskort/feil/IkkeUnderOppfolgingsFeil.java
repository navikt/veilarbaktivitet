package no.nav.veilarbaktivitet.aktivitetskort.feil;

import no.nav.veilarbaktivitet.oppfolging.siste_periode.IngenGjeldendePeriodeException;

public class IkkeUnderOppfolgingsFeil extends AktivitetsKortFunksjonellException {
    public IkkeUnderOppfolgingsFeil(IngenGjeldendePeriodeException cause) {
        super(new ErrorMessage("Bruker som ikke er under oppfølging kan ikke lage aktivitetskort"), cause);
    }
}
