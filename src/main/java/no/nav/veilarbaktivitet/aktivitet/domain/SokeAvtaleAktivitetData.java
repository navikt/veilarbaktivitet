package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
public class SokeAvtaleAktivitetData {
    public Long antallStillingerSokes;
    public Long antallStillingerIUken;
    public String avtaleOppfolging;
}
