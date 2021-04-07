package no.nav.veilarbaktivitet.arena;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;

@Data
@Builder(toBuilder = true)
public class ArenaForhaandsorienteringData {
	private String arenaktivitetId;
	private String aktorId;
	private Forhaandsorientering forhaandsorientering;
	private Date opprettetDato;
}
