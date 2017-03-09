package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class EndringsloggDTO {
    String endringsBeskrivelse;
    String endretAv;
    Date endretDato;
}
