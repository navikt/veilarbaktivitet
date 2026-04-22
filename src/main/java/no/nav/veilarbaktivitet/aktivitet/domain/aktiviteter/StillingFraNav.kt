package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData

class StillingFraNav(
    baseData: AktivitetBaseData,
    val stillingFraNavData: StillingFraNavData,
) : LestAktivitet(baseData) {

    class Opprett(
        val status: AktivitetStatus,
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val stillingFraNavData: StillingFraNavData,
    ): AktivitetsOpprettelse

    class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val stillingFraNavData: StillingFraNavData,
    ): AktivitetsEndring
}
