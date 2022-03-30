package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BrukernotifikasjonAktivitetIder {
    long id;
    long aktivitetId;
}
