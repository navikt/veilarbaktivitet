package no.nav.veilarbaktivitet.veilarbportefolje.dto;

import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;

public enum AktivitetTypeDTO {
    EGEN,
    STILLING,
    SOKEAVTALE,
    IJOBB,
    BEHANDLING,
    MOTE,
    SAMTALEREFERAT,
    STILLING_FRA_NAV,
    TILTAK;

    public static AktivitetTypeDTO fromDomainAktivitetType(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO domainAktivitetType, AktivitetskortType eksternAktivitetType) {
        return switch (domainAktivitetType) {

            case EGEN -> EGEN;
            case STILLING -> STILLING;
            case SOKEAVTALE -> SOKEAVTALE;
            case IJOBB -> IJOBB;
            case BEHANDLING -> BEHANDLING;
            case MOTE -> MOTE;
            case SAMTALEREFERAT -> SAMTALEREFERAT;
            case STILLING_FRA_NAV -> STILLING_FRA_NAV;
            // Foreløpig er alle eksternaktiviteter tiltak.
            case EKSTERNAKTIVITET -> {
                var result = switch (eksternAktivitetType) {
                    case MIDLERTIDIG_LONNSTILSKUDD  -> TILTAK;
                    case VARIG_LONNSTILSKUDD -> TILTAK;
                    case ARENA_TILTAK -> TILTAK;
                };
                yield result;
            }
        };

    }
}
