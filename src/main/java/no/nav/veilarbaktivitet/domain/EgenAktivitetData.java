package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import lombok.experimental.Wither;

@With
@Value
@Builder
@Getter
public class EgenAktivitetData {
    String hensikt;
    String oppfolging;
}
