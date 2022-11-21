package no.nav.veilarbaktivitet.aktivitetskort.dto;

import no.nav.veilarbaktivitet.person.InnsenderData;

public enum IdentType {
    ARENAIDENT,
    NAVIDENT,
    ORGNR,
    PERSONBRUKERIDENT;

    public InnsenderData mapToInnsenderType() {
        return switch (this) {
            case ARENAIDENT, NAVIDENT -> InnsenderData.NAV;
            case ORGNR -> InnsenderData.ORGNR;
            case PERSONBRUKERIDENT -> InnsenderData.BRUKER;
        };
    }
}
