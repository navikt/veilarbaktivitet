package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class MoteData {
    String adresse;
    String forberedelser;
    KanalDTO kanal;

    String referat;
    boolean referatPublisert;
}