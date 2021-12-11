package no.nav.veilarbaktivitet.aktivitet.domain;

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