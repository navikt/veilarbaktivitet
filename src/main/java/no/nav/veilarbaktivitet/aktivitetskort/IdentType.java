package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.person.InnsenderData;

public enum IdentType {
    ARENAIDENT;

    public InnsenderData mapToInnsenderType() {
        return switch (this) {
            case ARENAIDENT -> InnsenderData.NAV;
        };
    }
}
