package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;

import java.util.UUID;

@With
@Value
@Builder
public class MoteData {
    String adresse;
    String forberedelser;
    KanalDTO kanal;

    String referat;
    boolean referatPublisert;

    @Setter
    @NonFinal
    UUID oversiktenMeldingUuid;
}
