package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelingAvCvDTO {
    long aktivitetVersjon;
    boolean kanDeles;
}
