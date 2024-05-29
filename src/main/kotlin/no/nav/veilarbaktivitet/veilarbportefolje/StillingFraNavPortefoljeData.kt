package no.nav.veilarbaktivitet.veilarbportefolje;

import java.sql.Date;
import java.util.Objects;

public record StillingFraNavPortefoljeData(CvKanDelesStatus cvKanDelesStatus, Date svarfrist) {
    public static StillingFraNavPortefoljeData hvisStillingFraNavDataFinnes(Object id, Boolean cvKanDelesBoolean, Date svarfrist){
        if (Objects.isNull(id)) return null;

        CvKanDelesStatus cvKanDeles = CvKanDelesStatus.valueOf(cvKanDelesBoolean);

        return new StillingFraNavPortefoljeData(cvKanDeles, svarfrist);
    }
}
