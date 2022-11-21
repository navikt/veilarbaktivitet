package no.nav.veilarbaktivitet.aktivitet.dto;

import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.LenkeSeksjon;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Oppgaver;

import java.util.List;

public record EksternAktivitetDTO(
    AktivitetskortType type,
    Oppgaver oppgave,
    List<LenkeSeksjon> handlinger,
    List<Attributt> detaljer,
    List<Etikett> etiketter
) {
}
