package no.nav.veilarbaktivitet.aktivitet.dto;

import no.nav.veilarbaktivitet.aktivitetskort.*;

import java.util.List;

public record EksternAktivitetDTO(
    AktivitetskortType type,
    Oppgaver oppgave,
    List<LenkeSeksjon> handlinger,
    List<Attributt> detaljer,
    List<Etikett> etiketter
) {
}
