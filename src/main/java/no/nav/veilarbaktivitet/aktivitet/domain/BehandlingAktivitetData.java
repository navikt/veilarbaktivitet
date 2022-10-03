package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@EqualsAndHashCode
public class BehandlingAktivitetData {
    String behandlingType;
    String behandlingSted;
    String effekt;
    String behandlingOppfolging;
}