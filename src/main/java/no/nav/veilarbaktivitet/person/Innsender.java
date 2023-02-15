package no.nav.veilarbaktivitet.person;

import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.IdentType;

public enum Innsender {
    BRUKER,
    ARBEIDSGIVER,
    TILTAKSARRANGOER,
    NAV,
    SYSTEM,
    ARENAIDENT;

    public IdentType toIdentType() {
        return switch (this) {
            case NAV -> IdentType.NAVIDENT;
            case BRUKER -> IdentType.PERSONBRUKERIDENT;
            case ARBEIDSGIVER -> IdentType.ARBEIDSGIVER;
            case SYSTEM -> IdentType.SYSTEM;
            case TILTAKSARRANGOER -> IdentType.TILTAKSARRANGOER;
            case ARENAIDENT -> IdentType.ARENAIDENT;
        };
    }
}
