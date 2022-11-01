package no.nav.veilarbaktivitet.arena.model;

import lombok.*;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.FilterTag;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;

import java.util.Date;
import java.util.List;

@Data
@With
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ArenaAktivitetDTO {
    //Felles
    ArenaId id;
    Long aktivitetId;
    AktivitetStatus status;
    ArenaAktivitetTypeDTO type;
    String tittel;
    String beskrivelse;
    Date fraDato;
    Date tilDato;
    Date opprettetDato;
    boolean avtalt;
    ForhaandsorienteringDTO forhaandsorientering;
    public ArenaStatusDTO etikett;
    List<FilterTag> filterTags;

    // Tiltaksaktivitet
    Float deltakelseProsent;
    String tiltaksnavn;
    String tiltakLokaltNavn;
    String arrangoer;
    String bedriftsnummer;
    Float antallDagerPerUke;
    Date statusSistEndret;

    // Gruppeaktivitet
    List<MoteplanDTO> moeteplanListe;

}
