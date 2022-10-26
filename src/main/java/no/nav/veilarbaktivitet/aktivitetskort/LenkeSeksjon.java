package no.nav.veilarbaktivitet.aktivitetskort;

public record LenkeSeksjon(
        String tekst,
        String subtekst,
        String url,
        LenkeType type
) {
}
