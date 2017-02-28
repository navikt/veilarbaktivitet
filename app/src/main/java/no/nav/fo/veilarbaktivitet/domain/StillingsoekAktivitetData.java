package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StillingsoekAktivitetData {

    String arbeidsgiver;
    String stillingsTittel;
    String arbeidssted;
    StillingsoekEtikettData stillingsoekEtikett;
    String kontaktPerson;

}

