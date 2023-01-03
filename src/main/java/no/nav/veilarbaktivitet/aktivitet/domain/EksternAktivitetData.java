package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.LenkeSeksjon;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Oppgaver;

import java.util.List;

@Data
@Builder(toBuilder = true)
@With
public class EksternAktivitetData {
    String source;
    String tiltaksKode;
    AktivitetskortType type;
    Oppgaver oppgave;
    @Singular("handling")
    List<LenkeSeksjon> handlinger;
    @Singular("detalj")
    List<Attributt> detaljer;
    @Singular("etikett")
    List<Etikett> etiketter;
}
