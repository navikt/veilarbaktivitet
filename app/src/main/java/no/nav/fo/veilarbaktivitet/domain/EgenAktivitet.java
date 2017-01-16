package no.nav.fo.veilarbaktivitet.domain;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EgenAktivitet {
    public Aktivitet aktivitet;
}
