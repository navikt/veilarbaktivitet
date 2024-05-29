package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort

import java.net.URL

data class Oppgave(
    val tekst: String,
    val subtekst: String,
    val url: URL
)
