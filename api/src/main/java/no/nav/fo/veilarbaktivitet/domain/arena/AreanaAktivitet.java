package no.nav.fo.veilarbaktivitet.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;

import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class AreanaAktivitet {
    //Felles
    AktivitetStatus status; // bruk kodeverdi for å avgjøre denne
    AreanaAktivitetType aktivitetstype;
    String beskrivelse;
    Date fom;
    Date tom;

    // Tiltaksaktivitet
    float deltakelseProsent;
    String tiltaksnavn;
    String tiltakLokaltNavn;
    String arrangoer;
    String bedriftsnummer;
    float antallDagerPerUke;
    //usikker på deltakerStatus
    Date statusSistEndret;

    // Gruppeaktivitet
    List<Moteplan> moeteplanListe;

}
