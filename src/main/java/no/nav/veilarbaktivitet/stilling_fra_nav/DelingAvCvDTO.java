package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DelingAvCvDTO {
    long aktivitetVersjon;
    boolean kanDeles;
    LocalDate avtaltDato;
}
