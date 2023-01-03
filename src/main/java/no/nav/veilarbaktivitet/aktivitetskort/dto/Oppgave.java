package no.nav.veilarbaktivitet.aktivitetskort.dto;

import java.net.URL;

public record Oppgave(
    String tekst,
    String subtekst,
    URL url,
    String knapptekst
) {
}
