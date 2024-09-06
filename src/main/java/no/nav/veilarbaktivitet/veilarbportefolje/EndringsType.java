package no.nav.veilarbaktivitet.veilarbportefolje;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;

public enum EndringsType {
    OPPRETTET,
    FLYTTET,
    REDIGERT,
    HISTORISK;

    public static EndringsType get(AktivitetTransaksjonsType transaksjonsType) {
        return switch (transaksjonsType) {
            case OPPRETTET -> OPPRETTET;
            case BLE_HISTORISK, KASSERT -> HISTORISK;
            case STATUS_ENDRET -> FLYTTET;
            case DETALJER_ENDRET, AVTALT, AVTALT_DATO_ENDRET, ETIKETT_ENDRET, MOTE_TID_OG_STED_ENDRET, REFERAT_OPPRETTET, REFERAT_ENDRET, REFERAT_PUBLISERT, FORHAANDSORIENTERING_LEST, DEL_CV_SVART, SOKNADSSTATUS_ENDRET, IKKE_FATT_JOBBEN, FATT_JOBBEN
                    -> REDIGERT;
        };
    }

}
