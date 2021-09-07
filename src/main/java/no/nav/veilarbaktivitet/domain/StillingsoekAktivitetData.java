package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.experimental.Wither;

@With
@Value
@Builder
public class StillingsoekAktivitetData {

    String arbeidsgiver;
    String stillingsTittel;
    String arbeidssted;
    StillingsoekEtikettData stillingsoekEtikett;
    String kontaktPerson;

}

