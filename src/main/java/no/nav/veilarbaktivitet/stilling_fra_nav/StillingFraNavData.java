package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.Date;

@Data
@Builder(toBuilder = true)
@With
public class StillingFraNavData {
    CvKanDelesData cvKanDelesData;
    String soknadsfrist;
    Date svarfrist;
    String arbeidsgiver;
    String bestillingsId;
    String stillingsId;
    String arbeidssted;
    KontaktpersonData kontaktpersonData;
    Soknadsstatus soknadsstatus;
    LivslopsStatus livslopsStatus;
    String varselId; // TODO fjerne denne?
}
