package no.nav.veilarbaktivitet.aktivitet.aktivitet_typer;

import lombok.Builder;
import lombok.Value;
import lombok.With;

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

