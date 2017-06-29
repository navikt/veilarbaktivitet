package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BehandlingAktivitetData {
    String behandlingSted;
    String effekt;
    String behandlingOppfolging;
}