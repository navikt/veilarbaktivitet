package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

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
            case NAVIDENT-> Innsender.NAV;
            case SYSTEM -> Innsender.SYSTEM;
            case PERSONBRUKERIDENT -> Innsender.BRUKER;
            case ARBEIDSGIVER -> Innsender.ARBEIDSGIVER;
            case TILTAKSARRANGOER -> Innsender.TILTAKSARRANGOER;
        };
    }
}
