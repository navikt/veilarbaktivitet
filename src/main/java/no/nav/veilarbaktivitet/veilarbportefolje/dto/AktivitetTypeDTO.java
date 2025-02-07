package no.nav.veilarbaktivitet.veilarbportefolje.dto;

import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;

import java.util.HashMap;
import java.util.Map;

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

    private static final Map<AktivitetskortType, String> aktivitetskortTypeTilArenaTiltakskode = new HashMap<>();
    static {
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD, "MIDLONTIL");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.VARIG_LONNSTILSKUDD, "VARLONTIL");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.ARBEIDSTRENING, "ARBTREN");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET, "VATIAROR");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.INDOPPFAG, "INDOPPFAG");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.ARBFORB, "ARBFORB");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.AVKLARAG, "AVKLARAG");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.VASV, "VASV");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.ARBRRHDAG, "ARBRRHDAG");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.DIGIOPPARB, "DIGIOPPARB");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.JOBBK, "JOBBK");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.GRUPPEAMO, "GRUPPEAMO");
        aktivitetskortTypeTilArenaTiltakskode.put(AktivitetskortType.GRUFAGYRKE, "GRUFAGYRKE");
    }

    public static String aktivitetsKortTypeToArenaTiltakskode(AktivitetskortType aktivitetskortType) {
        return aktivitetskortTypeTilArenaTiltakskode.get(aktivitetskortType);
    }

    public static AktivitetTypeDTO fromDomainAktivitetType(no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO domainAktivitetType) {
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
            case EKSTERNAKTIVITET -> TILTAK;
        };

    }
}
