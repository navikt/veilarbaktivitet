package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
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

fun StillingsoekEtikettData?.toArkivEtikettStil(): ArkivEtikettStil {
    return when(this) {
        StillingsoekEtikettData.SOKNAD_SENDT -> ArkivEtikettStil.NEUTRAL
        StillingsoekEtikettData.INNKALT_TIL_INTERVJU -> ArkivEtikettStil.NEUTRAL
        StillingsoekEtikettData.AVSLAG -> ArkivEtikettStil.NEGATIVE
        StillingsoekEtikettData.JOBBTILBUD -> ArkivEtikettStil.NEGATIVE
        else -> ArkivEtikettStil.NEUTRAL
    }
}

fun Sentiment?.toArkivEtikettStil(): ArkivEtikettStil {
    return when(this) {
        Sentiment.NEUTRAL -> ArkivEtikettStil.NEUTRAL
        Sentiment.NEGATIVE -> ArkivEtikettStil.NEGATIVE
        Sentiment.POSITIVE -> ArkivEtikettStil.POSITIVE
        else -> ArkivEtikettStil.NEUTRAL
    }
}
