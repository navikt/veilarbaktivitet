package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@With
public class Aktivitetskort {
    UUID id;
    String personIdent;
    String tittel;
    String beskrivelse;
    AktivitetStatus aktivitetStatus;
    LocalDate startDato;
    LocalDate sluttDato;
    IdentDTO endretAv;
    LocalDateTime endretTidspunkt;
    Boolean avtaltMedNav;
    // String avsluttetBegrunnelse;

    OppgaveLenke oppgave;
    LenkeSeksjon[] handlinger;
    Attributt[] detaljer;
    Etikett[] etiketter;
}
