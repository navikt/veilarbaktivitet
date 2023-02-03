package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort;

public record Oppgaver(
    Oppgave ekstern,
    Oppgave intern
) {}
