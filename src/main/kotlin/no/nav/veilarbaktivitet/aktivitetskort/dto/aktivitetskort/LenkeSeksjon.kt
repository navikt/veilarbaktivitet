package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort

import java.net.URL

data class LenkeSeksjon(
    val tekst: String,
    val subtekst: String,
    val url: URL,
    val lenkeType: LenkeType
)
