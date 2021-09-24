package no.nav.veilarbaktivitet.aktivitet.aktivitet_typer;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
@Getter
public class EgenAktivitetData {
    String hensikt;
    String oppfolging;
}
