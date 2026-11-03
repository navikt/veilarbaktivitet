package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;

import java.util.List;

@With
@Value
@Builder
public class MoteData {
    String adresse;
    String forberedelser;
    KanalDTO kanal;
    String referat;
    boolean referatPublisert;
    List<Motedeltaker> motedeltakere;
}
