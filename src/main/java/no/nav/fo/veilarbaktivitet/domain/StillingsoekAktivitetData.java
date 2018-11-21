package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
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

