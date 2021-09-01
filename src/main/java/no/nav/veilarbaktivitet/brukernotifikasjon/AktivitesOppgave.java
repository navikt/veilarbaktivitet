package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Data;
import no.nav.veilarbaktivitet.domain.Person;

import java.util.UUID;

@Data
public class AktivitesOppgave {
    private final long aktivitetId;
    private final Person.Fnr fnr;
    private final String tekst;
    private final UUID varselId;
    private final String oppfolgingsPeriode;
}
