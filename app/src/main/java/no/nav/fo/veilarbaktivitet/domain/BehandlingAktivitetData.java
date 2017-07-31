package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class BehandlingAktivitetData {
    String behandlingType;
    String behandlingSted;
    String effekt;
    String behandlingOppfolging;
}