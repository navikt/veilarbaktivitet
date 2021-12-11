package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class DelingAvCvDTO {
    long aktivitetVersjon;
    boolean kanDeles;
    Date avtaltDato;
}
