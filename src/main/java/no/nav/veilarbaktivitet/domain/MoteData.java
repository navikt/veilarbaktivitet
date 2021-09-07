package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.experimental.Wither;

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