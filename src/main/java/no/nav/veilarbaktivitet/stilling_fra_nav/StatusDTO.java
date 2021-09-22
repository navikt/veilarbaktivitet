package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Data;

@Data
public class StatusDTO {
    String aktivitetVersion;
    SoknadsProssesStatus soknadsProssesStatus;
}
