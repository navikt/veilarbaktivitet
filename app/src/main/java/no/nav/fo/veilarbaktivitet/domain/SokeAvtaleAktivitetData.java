package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SokeAvtaleAktivitetData {
    public Long antall;
    public String avtaleOppfolging;
}
