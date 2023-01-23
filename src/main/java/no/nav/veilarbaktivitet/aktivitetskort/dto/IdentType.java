package no.nav.veilarbaktivitet.aktivitetskort.dto;

import no.nav.veilarbaktivitet.person.Innsender;

public enum IdentType {
    ARENAIDENT,
    NAVIDENT,
    TILTAKSARRANGOER,
    ARBEIDSGIVER,
    SYSTEM,
    PERSONBRUKERIDENT;

    public Innsender toInnsender() {
        return switch (this) {
            case ARENAIDENT -> Innsender.ARENAIDENT;
            case NAVIDENT, SYSTEM -> Innsender.NAV;
            case PERSONBRUKERIDENT -> Innsender.BRUKER;
            case ARBEIDSGIVER -> Innsender.ARBEIDSGIVER;
            case TILTAKSARRANGOER -> Innsender.TILTAKSARRANGOER;
        };
    }
}
