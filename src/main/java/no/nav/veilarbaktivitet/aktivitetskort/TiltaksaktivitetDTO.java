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
public class TiltaksaktivitetDTO {
    // aktivitetdata
    UUID id;
    String eksternReferanseId;
    String personIdent;
    LocalDate startDato;
    LocalDate sluttDato;
    String tittel;
    String beskrivelse;
    AktivitetStatus aktivitetStatus;
    IdentDTO endretAv;
    LocalDateTime endretDato;

    // tiltaksaktivitetdata spesifikt
    String arrangoernavn;
    String tiltaksNavn;
    String tiltaksKode;
    String deltakelseStatus;

    @Singular("detalj")
    Map<String, String> detaljer;
}
