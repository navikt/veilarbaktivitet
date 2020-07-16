package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class SokeAvtaleAktivitetData {
    public Long antallStillingerSokes;
    public Long antallStillingerIUken;
    public String avtaleOppfolging;
}
