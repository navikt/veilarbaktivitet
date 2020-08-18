package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
@Getter
public class EgenAktivitetData {
    String hensikt;
    String oppfolging;
}
