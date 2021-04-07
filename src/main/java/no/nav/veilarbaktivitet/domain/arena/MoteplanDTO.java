package no.nav.veilarbaktivitet.domain.arena;

import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MoteplanDTO {
	Date startDato; //startKlokkeslett kan også være i denne
	Date sluttDato;
	String sted;
}
