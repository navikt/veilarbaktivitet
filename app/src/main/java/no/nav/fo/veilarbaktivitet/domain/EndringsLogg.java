package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class EndringsLogg {

    long id;
    String endringsBeskrivelse;
    String endretAv;
    Date endretDato;

}

