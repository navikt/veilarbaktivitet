package no.nav.veilarbaktivitet.aktivitetskort.dto;

import no.nav.veilarbaktivitet.person.Innsender;

public enum IdentType {
    ARENAIDENT,
    NAVIDENT,
    TILTAKSARRANGOER,
    ARBEIDSGIVER,
    PERSONBRUKERIDENT;

    public Innsender mapToInnsenderType() {
        return switch (this) {
            case ARENAIDENT, NAVIDENT -> Innsender.NAV;
            case PERSONBRUKERIDENT -> Innsender.BRUKER;
            case ARBEIDSGIVER -> Innsender.ARBEIDSGIVER;
            case TILTAKSARRANGOER -> Innsender.TILTAKSARRAGOER;
        };
    }
}
