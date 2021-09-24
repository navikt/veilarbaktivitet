package no.nav.veilarbaktivitet.aktivitet.aktivitet_typer;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
public class BehandlingAktivitetData {
    String behandlingType;
    String behandlingSted;
    String effekt;
    String behandlingOppfolging;
}