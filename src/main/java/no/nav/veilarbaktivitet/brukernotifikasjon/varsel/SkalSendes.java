package no.nav.veilarbaktivitet.brukernotifikasjon.varsel;

import lombok.Builder;
import lombok.Getter;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;

import java.net.URL;
import java.util.UUID;

@Getter
@Builder
public class SkalSendes {
    private final long brukernotifikasjonLopeNummer;
    private final UUID brukernotifikasjonId;
    private final VarselType varselType;
    private final String melding;
    private final String oppfolgingsperiode;
    private final Person.Fnr fnr;
    private final String epostTitel;
    private final String epostBody;
    private final String smsTekst;
    private final URL url;
}
