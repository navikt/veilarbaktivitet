package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class SkalAvluttes {
    private final String brukernotifikasjonId; // Samme som varselId
    private final Person.Fnr fnr;
    private final UUID oppfolgingsperiode;
}
