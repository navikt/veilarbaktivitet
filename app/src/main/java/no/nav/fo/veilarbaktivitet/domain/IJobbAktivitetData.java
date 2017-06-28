package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IJobbAktivitetData {
    JobbStatusTypeData jobbStatusType;
    String ansttelsesforhold;
    String arbeidstid;
}