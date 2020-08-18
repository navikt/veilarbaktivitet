package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

@Value
@Builder
@Wither
public class StillingsoekAktivitetData {

    public String arbeidsgiver;
    String stillingsTittel;
    String arbeidssted;
    StillingsoekEtikettData stillingsoekEtikett;
    String kontaktPerson;

}

