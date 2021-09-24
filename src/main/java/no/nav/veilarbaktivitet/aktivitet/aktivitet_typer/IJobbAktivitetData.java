package no.nav.veilarbaktivitet.aktivitet.aktivitet_typer;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
public class IJobbAktivitetData {
    JobbStatusTypeData jobbStatusType;
    String ansettelsesforhold;
    String arbeidstid;
}