package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class SkalSendes {
    private final long id;
    private final String brukernotifikasjonId;
    private final long aktivitetId;
    private final String melding;
    private final String oppfolgingsperiode;
    private final String aktorId;
}
