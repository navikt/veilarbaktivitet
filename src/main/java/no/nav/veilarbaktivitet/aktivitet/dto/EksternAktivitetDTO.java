package no.nav.veilarbaktivitet.aktivitet.dto;

import no.nav.veilarbaktivitet.aktivitetskort.*;

public record EksternAktivitetDTO(
    AktivitetskortType type,
    OppgaveLenke oppgave,
    LenkeSeksjon[] handlinger,
    Attributt[] detaljer,
    Etikett[] etiketter
) {
}
