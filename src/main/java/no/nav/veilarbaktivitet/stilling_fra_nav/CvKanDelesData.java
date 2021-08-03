package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.*;
import no.nav.veilarbaktivitet.domain.InnsenderData;

import java.util.Date;

@Builder
@With
@Getter
@Data
public class CvKanDelesData {
    Boolean kanDeles;
    Date endretTidspunkt;
    String endretAv;
    InnsenderData endretAvType;
}
