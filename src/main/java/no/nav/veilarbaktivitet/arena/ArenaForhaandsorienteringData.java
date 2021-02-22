package no.nav.veilarbaktivitet.arena;

import lombok.Builder;
import lombok.Data;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;

import java.util.Date;

@Data
@Builder(toBuilder = true)
public class ArenaForhaandsorienteringData {
    private String arenaktivitetId;
    private String aktorId;
    private Forhaandsorientering forhaandsorientering;
    private Date opprettetDato;
}
