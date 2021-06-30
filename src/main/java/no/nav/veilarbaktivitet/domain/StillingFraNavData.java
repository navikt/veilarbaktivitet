package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.Date;

@Data
@Builder
@With
public class StillingFraNavData {
    Boolean kanDeles;
    Date cvKanDelesTidspunkt;
    String cvKanDelesAv;
    InnsenderData cvKanDelesAvType;
    String soknadsfrist;
    Date svarfrist;
    String arbeidsgiver;
    String bestillingsId;
    String stillingsId;
    String arbeidssted;
    String varselId;
}
