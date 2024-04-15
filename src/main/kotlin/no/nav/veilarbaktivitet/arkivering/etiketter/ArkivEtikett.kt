package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Sentiment

data class ArkivEtikett(
    val stil: ArkivEtikettStil,
    val tekst: String
)

enum class ArkivEtikettStil {
    AVTALT, // Avtalt - hardkodet farge i frontend
    // Disse kommer fra AKAAS
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}

fun Sentiment?.toArkivEtikettStil(): ArkivEtikettStil {
    return when(this) {
        Sentiment.NEUTRAL -> ArkivEtikettStil.NEUTRAL
        Sentiment.NEGATIVE -> ArkivEtikettStil.NEGATIVE
        Sentiment.POSITIVE -> ArkivEtikettStil.POSITIVE
        else -> ArkivEtikettStil.NEUTRAL
    }
}
