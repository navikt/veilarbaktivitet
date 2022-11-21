package no.nav.veilarbaktivitet.aktivitetskort;

import java.net.URL;

public record LenkeSeksjon(
        String tekst,
        String subtekst,
        URL url,
        LenkeType type
) {
}
