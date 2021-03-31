package no.nav.veilarbaktivitet.domain.kafka;

import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;

public enum EndringsType {
    OPPRETTET,
    FLYTTET,
    REDIGERT,
    HISTORISK;

    public static EndringsType get(AktivitetTransaksjonsType transaksjonsType) {
        switch (transaksjonsType) {
            case OPPRETTET:
                return OPPRETTET;
            case BLE_HISTORISK:
                return HISTORISK;
            case STATUS_ENDRET:
                return FLYTTET;
            case DETALJER_ENDRET:
            case AVTALT:
            case AVTALT_DATO_ENDRET:
            case ETIKETT_ENDRET:
            case MOTE_TID_OG_STED_ENDRET:
            case REFERAT_OPPRETTET:
            case REFERAT_ENDRET:
            case REFERAT_PUBLISERT:
                return REDIGERT;
            default:
                throw new IllegalArgumentException(transaksjonsType.toString());
        }
    }

}
