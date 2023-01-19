package no.nav.veilarbaktivitet.aktivitetskort.dto;

import io.swagger.models.auth.In;
import no.nav.veilarbaktivitet.person.InnsenderData;

public enum IdentType {
    ARENAIDENT,
    NAVIDENT,
    TILTAKSARRANGOER,
    ARBEIDSGIVER,
    PERSONBRUKERIDENT;

    public InnsenderData mapToInnsenderType() {
        return switch (this) {
            case ARENAIDENT, NAVIDENT -> InnsenderData.NAV;
            case PERSONBRUKERIDENT -> InnsenderData.BRUKER;
            case ARBEIDSGIVER -> InnsenderData.ARBEIDSGIVER;
        };
    }
}
