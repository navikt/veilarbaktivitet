package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekAktivitetData

class Jobbsoeking(
    baseData: AktivitetBaseData,
    val stillingsSoekAktivitetData: StillingsoekAktivitetData,
) : LestAktivitet(baseData) {

    class Opprett(
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val stillingsSoekAktivitetData: StillingsoekAktivitetData,
    ): AktivitetsOpprettelse

    class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val stillingsSoekAktivitetData: StillingsoekAktivitetData,
    ): AktivitetsEndring
}
