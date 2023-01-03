package no.nav.veilarbaktivitet.veilarbportefolje;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;

public enum CvKanDelesStatus {
    JA,
    NEI,
    IKKE_SVART;

    public static CvKanDelesStatus valueOf(Boolean cvKanDeles) {
        if (isNull(cvKanDeles)) return IKKE_SVART;
        else if (TRUE.equals(cvKanDeles)) return JA;
        else return NEI;
    }
}
