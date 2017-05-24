package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EgenAktivitetData {
    String hensikt;
    String oppfolging;
}
