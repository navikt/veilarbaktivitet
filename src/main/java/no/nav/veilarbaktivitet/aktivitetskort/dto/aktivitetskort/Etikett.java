package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

public record Etikett(
        String tekst,
        Sentiment sentiment,
        String kode
) {
}
