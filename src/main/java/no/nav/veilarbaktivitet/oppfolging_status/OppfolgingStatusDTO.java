package no.nav.veilarbaktivitet.oppfolging_status;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OppfolgingStatusDTO {

    private boolean underOppfolging;

    private boolean erManuell;
}
