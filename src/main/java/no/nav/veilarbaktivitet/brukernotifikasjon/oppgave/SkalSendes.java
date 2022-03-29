package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.Builder;
import lombok.Getter;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;

import java.net.URL;

@Getter
@Builder
class SkalSendes {
    private final long brukernotifikasjonLopeNummer;
    private final String brukernotifikasjonId;
    private final VarselType varselType;
    private final String melding;
    private final String oppfolgingsperiode;
    private final Person.Fnr fnr;
    private final String epostTitel;
    private final String epostBody;
    private final String smsTekst;
    private final URL url;
}
