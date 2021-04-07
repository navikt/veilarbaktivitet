package no.nav.veilarbaktivitet.domain.arena;

import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;

@Data
@Accessors(chain = true)
public class ArenaAktivitetDTO {
	//Felles
	String id;
	AktivitetStatus status;
	ArenaAktivitetTypeDTO type;
	String tittel;
	String beskrivelse;
	Date fraDato;
	Date tilDato;
	Date opprettetDato;
	boolean avtalt;
	Forhaandsorientering forhaandsorientering;
	public ArenaStatusDTO etikett;

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
