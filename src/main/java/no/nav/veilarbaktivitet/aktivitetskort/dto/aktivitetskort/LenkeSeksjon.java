package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

import java.net.URL;

public record LenkeSeksjon(
        String tekst,
        String subtekst,
        URL url,
        LenkeType lenkeType
) {
}
