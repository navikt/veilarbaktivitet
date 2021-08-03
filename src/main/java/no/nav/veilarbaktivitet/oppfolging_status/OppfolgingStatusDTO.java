package no.nav.veilarbaktivitet.oppfolging_status;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
/*
  Respons fra api/oppfolging
  De fleste attributter er ikke tatt med, slik som oppfølgingsperioder, eskaleringsvarsel, etc, siden vi ikke trenger disse i dag.
 */
public class OppfolgingStatusDTO {

    private String fnr;
    private boolean reservasjonKRR;
    private boolean manuell;
    private boolean underOppfolging;
    private boolean underKvp;
}
