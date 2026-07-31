package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

data class ReferatEndring(
    val id: Long,
    val versjon: Long,
    val sporingsData: SporingsData,
    val moteData: MoteData
) {
    fun kanEndreAktivitetGuard(orginalAktivitet: AktivitetData, sisteVersjon: Long) {
        if (
            moteData.isReferatPublisert
            && !orginalAktivitet.moteData.isReferatPublisert
            && moteData == orginalAktivitet.moteData.withReferatPublisert(true)
        ) {
            return
        }
        if (orginalAktivitet.versjon != sisteVersjon) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        } else if (!orginalAktivitet.endringTillatt()) {
            throw EndringAvFerdigAktivitetException("Kan ikke endre aktivitet i en ferdig status")
        }
    }
}
