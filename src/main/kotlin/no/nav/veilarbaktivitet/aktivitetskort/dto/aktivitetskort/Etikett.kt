package no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort

data class Etikett(
    val tekst: String,
    val sentiment: Sentiment?,
    val kode: String
)
