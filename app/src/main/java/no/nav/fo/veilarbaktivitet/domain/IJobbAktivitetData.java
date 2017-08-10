package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class IJobbAktivitetData {
    JobbStatusTypeData jobbStatusType;
    String ansettelsesforhold;
    String arbeidstid;
}