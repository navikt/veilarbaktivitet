package no.nav.veilarbaktivitet.aktivitet.dto;

import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver;

import java.util.List;

public record EksternAktivitetDTO(
    AktivitetskortType type,
    Oppgaver oppgave,
    List<LenkeSeksjon> handlinger,
    List<Attributt> detaljer,
    List<Etikett> etiketter
) {
}
