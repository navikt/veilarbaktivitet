package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SokeAvtaleAktivitetData {
    public Long antallStillingerSokes;
    public String avtaleOppfolging;
}
