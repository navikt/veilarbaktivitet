package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Stillingsoek {

    String arbeidsgiver;
    String stillingsTittel;
    StillingsoekEtikett stillingsoekEtikett;
    String kontaktPerson;

}

