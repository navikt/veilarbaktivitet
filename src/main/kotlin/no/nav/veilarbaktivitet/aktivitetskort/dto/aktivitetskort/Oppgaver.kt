package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort

data class Oppgaver(
    val ekstern: Oppgave,
    val intern: Oppgave?
)
