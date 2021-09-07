package no.nav.veilarbaktivitet.avtalt_med_nav;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AvtaltMedNavDTO {
    private long aktivitetVersjon;
    private ForhaandsorienteringDTO forhaandsorientering;
}
