package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.Builder;
import lombok.Getter;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;

@Getter
@Builder
class SkalSendes {
    private final long brukernotifikasjonLopeNummer;
    private final String brukernotifikasjonId;
    private final VarselType varselType;
    private final long aktivitetId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String aktorId;
    private final String epostTitel;
    private final String epostBody;
    private final String smsTekst;
}
