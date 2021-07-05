package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;
import lombok.With;
import java.util.Date;

@Data
@Builder
@With
public class StillingFraNavData {
    CvKanDelesData cvKanDelesData;
    String soknadsfrist;
    Date svarfrist;
    String arbeidsgiver;
    String bestillingsId;
    String stillingsId;
    String arbeidssted;
    String varselId;
}
