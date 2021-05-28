package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AvtaltMedNavDTO {
    private long aktivitetVersjon;
    private ForhaandsorienteringDTO forhaandsorientering;
}
