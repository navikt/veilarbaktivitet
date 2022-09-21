package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.util.Date;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@With
public class TiltaksaktivitetDTO {
    // aktivitetdata
    UUID funksjonellId;
    String personIdent;
    String tittel;
    Date startDato;
    Date sluttDato;
    String beskrivelse;
    StatusDTO statusDTO;

    // tiltaksaktivitetdata spesifikt
    TiltakDTO tiltakDTO;
    String arrangornavn;
    Integer deltakelsesprosent;
    Integer dagerPerUke;
    Date registrertDato;
    Date statusEndretDato;
}
