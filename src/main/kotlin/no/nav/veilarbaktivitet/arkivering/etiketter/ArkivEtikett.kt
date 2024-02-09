package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Sentiment
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus

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

fun StillingsoekEtikettData.toArkivEtikettString(): String? {
    return when {
        this == StillingsoekEtikettData.SOKNAD_SENDT -> "Sendt søknad og venter på svar"
        this == StillingsoekEtikettData.AVSLAG -> "Ikke fått jobben"
        this == StillingsoekEtikettData.INNKALT_TIL_INTERVJU -> "Fått jobbtilbud \uD83C\uDF89"
        this == StillingsoekEtikettData.JOBBTILBUD -> "Fått jobben"
        else -> null
    }
}

fun Soknadsstatus.toArkivEtikettString(): String? {
    return when(this) {
        Soknadsstatus.VENTER -> "Venter på å bli kontaktet"
        Soknadsstatus.CV_DELT -> "CV er delt med arbeidsgiver"
        Soknadsstatus.SKAL_PAA_INTERVJU -> "Skal på intervju"
        Soknadsstatus.JOBBTILBUD -> "Fått jobbtilbud \uD83C\uDF89"
        Soknadsstatus.AVSLAG -> "Ikke fått jobben"
        Soknadsstatus.IKKE_FATT_JOBBEN -> "Ikke fått jobben"
        Soknadsstatus.FATT_JOBBEN -> "Fått jobben \uD83C\uDF89"
    }
}

fun Sentiment.toArkivEtikettStil(): ArkivEtikettStil {
    return when(this) {
        Sentiment.NEUTRAL -> ArkivEtikettStil.NEUTRAL
        Sentiment.NEGATIVE -> ArkivEtikettStil.NEGATIVE
        Sentiment.POSITIVE -> ArkivEtikettStil.POSITIVE
    }
}