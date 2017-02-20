package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StillingsoekAktivitetData {

    String arbeidsgiver;
    String stillingsTittel;
    StillingsoekEtikettData stillingsoekEtikett;
    String kontaktPerson;

}

