package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.experimental.Wither;

@With
@Value
@Builder
public class SokeAvtaleAktivitetData {
    public Long antallStillingerSokes;
    public Long antallStillingerIUken;
    public String avtaleOppfolging;
}
