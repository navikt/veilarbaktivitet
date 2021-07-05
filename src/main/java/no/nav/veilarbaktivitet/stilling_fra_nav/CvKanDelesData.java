package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import no.nav.veilarbaktivitet.domain.InnsenderData;

import java.util.Date;

@Builder
@With
@Getter
public class CvKanDelesData {
    Boolean kanDeles;
    Date endretTidspunkt;
    String endretAv;
    InnsenderData endretAvType;

}
