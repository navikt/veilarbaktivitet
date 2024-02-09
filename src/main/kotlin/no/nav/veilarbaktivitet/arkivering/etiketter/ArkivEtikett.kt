package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
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
    return when {
        this == Soknadsstatus.AVSLAG -> "Sendt søknad og venter på svar"
        this == Soknadsstatus.AVSLAG -> "Ikke fått jobben"
        this == Soknadsstatus.CV_DELT -> "Fått jobbtilbud \uD83C\uDF89"
        this == Soknadsstatus.IKKE_FATT_JOBBEN -> "Fått jobben"
        else -> null
    }
}