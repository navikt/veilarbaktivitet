package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Data;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitetskort.*;

@Data
@Builder(toBuilder = true)
@With
public class EksternAktivitetData {
    String source;
    String tiltaksKode;
    AktivitetskortType type;
    OppgaveLenke oppgave;
    LenkeSeksjon[] handlinger;
    Attributt[] detaljer;
    Etikett[] etiketter;
}
