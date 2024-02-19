package no.nav.veilarbaktivitet.aktivitetskort.dto

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus

enum class AktivitetskortStatus {
    FORSLAG,
    PLANLAGT,
    GJENNOMFORES,
    FULLFORT,
    AVBRUTT;

    fun toAktivitetStatus(): AktivitetStatus {
        return when (this) {
            FORSLAG -> AktivitetStatus.BRUKER_ER_INTERESSERT
            PLANLAGT -> AktivitetStatus.PLANLAGT
            GJENNOMFORES -> AktivitetStatus.GJENNOMFORES
            FULLFORT -> AktivitetStatus.FULLFORT
            AVBRUTT -> AktivitetStatus.AVBRUTT
        }
    }
}
