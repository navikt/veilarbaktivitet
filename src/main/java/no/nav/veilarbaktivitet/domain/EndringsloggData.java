package no.nav.veilarbaktivitet.domain;

import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EndringsloggData {
	String endringsBeskrivelse;
	String endretAv;
	Date endretDato;
}
