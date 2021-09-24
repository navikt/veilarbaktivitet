package no.nav.veilarbaktivitet.aktivitet.aktivitet_typer;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
public class MoteData {
    String adresse;
    String forberedelser;
    KanalDTO kanal;

    String referat;
    boolean referatPublisert;
}