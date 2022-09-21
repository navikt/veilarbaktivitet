package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;

@Builder
public class StatusDTO {
    AktivitetStatus status;
    String aarsak;
}
