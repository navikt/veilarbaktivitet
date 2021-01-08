package no.nav.veilarbaktivitet.aktiviterTilKafka;

import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.domain.InnsenderData;

public enum SisteEndringKategori {
    NY_STILLING,
    NY_IJOBB,
    NY_EGEN,
    NY_BEHANDLING,

    FULLFORT_STILLING,
    FULLFORT_IJOBB,
    FULLFORT_EGEN,
    FULLFORT_BEHANDLING,
    FULLFORT_SOKEAVTALE,

    AVBRUTT_STILLING,
    AVBRUTT_IJOBB,
    AVBRUTT_EGEN,
    AVBRUTT_BEHANDLING,
    AVBRUTT_SOKEAVTALE;

    public static SisteEndringKategori getKategori( AktivitetStatus aktivitetStatus, AktivitetTypeDTO aktivitetType, AktivitetTransaksjonsType transaksjonsType) {
        String potensiellSisteEndringsKategori = null;
        if (transaksjonsType.equals(AktivitetTransaksjonsType.OPPRETTET)) {
            potensiellSisteEndringsKategori = "NY_"+aktivitetType.name();
        } else if (transaksjonsType.equals(AktivitetTransaksjonsType.STATUS_ENDRET)){
            potensiellSisteEndringsKategori = aktivitetStatus + "_" + aktivitetType.name();
        }

        if(contains(potensiellSisteEndringsKategori)){
            return SisteEndringKategori.valueOf(potensiellSisteEndringsKategori);
        }
        return null;
    }

    public static boolean contains(String value) {
        try {
            SisteEndringKategori.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
