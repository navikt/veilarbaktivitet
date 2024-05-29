package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

import java.net.URL;

public record Oppgave(
    String tekst,
    String subtekst,
    URL url
) {
}
