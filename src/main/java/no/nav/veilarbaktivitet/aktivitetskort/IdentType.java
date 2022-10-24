package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.person.InnsenderData;

public enum IdentType {
    ARENAIDENT,
    NAVIDENT,
    PERSONBRUKERIDENT;

    public InnsenderData mapToInnsenderType() {
        return switch (this) {
            case ARENAIDENT, NAVIDENT -> InnsenderData.NAV;
            case PERSONBRUKERIDENT -> InnsenderData.BRUKER;
        };
    }
}
