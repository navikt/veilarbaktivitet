package no.nav.veilarbaktivitet.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ArenaAktivitetDTO {
    //Felles
    String id;
    AktivitetStatus status;
    ArenaAktivitetTypeDTO type;
    String tittel;
    String beskrivelse;
    ZonedDateTime fraDato;
    ZonedDateTime tilDato;
    ZonedDateTime opprettetDato;
    boolean avtalt;
    public ArenaStatusDTO etikett;

    // Tiltaksaktivitet
    Float deltakelseProsent;
    String tiltaksnavn;
    String tiltakLokaltNavn;
    String arrangoer;
    String bedriftsnummer;
    Float antallDagerPerUke;
    ZonedDateTime statusSistEndret;

    // Gruppeaktivitet
    List<MoteplanDTO> moeteplanListe;

}
