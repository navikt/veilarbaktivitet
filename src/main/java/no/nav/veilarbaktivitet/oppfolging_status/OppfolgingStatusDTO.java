package no.nav.veilarbaktivitet.oppfolging_status;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@Builder
/*
  Respons fra api/oppfolging
  Noen attributter er ikke tatt med, slik som oppfølgingsperioder og eskaleringsvarsel, siden vi ikke trenger disse per nå.
 */
public class OppfolgingStatusDTO {

    private String fnr;
    private String aktorId;
    private String veilederId;
    private boolean reservasjonKRR;
    private boolean kanVarsles;
    private boolean manuell;
    private boolean underOppfolging;
    private boolean underKvp;

    private ZonedDateTime oppfolgingUtgang;
    private boolean kanStarteOppfolging;
    private boolean harSkriveTilgang;
    private Boolean inaktivIArena;
    private Boolean kanReaktiveres;
    private LocalDate inaktiveringsdato;
    private Boolean erSykmeldtMedArbeidsgiver;
    private String servicegruppe;
    private String formidlingsgruppe;
    private String rettighetsgruppe;
}
