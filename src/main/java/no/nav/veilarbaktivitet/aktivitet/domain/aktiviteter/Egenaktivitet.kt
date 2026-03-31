package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.EgenAktivitetData

class Egenaktivitet {

    class Opprett(
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sproring: SporingsData,
        val egenAktivitetData: EgenAktivitetData,
    ): AktivitetsOpprettelse

    class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val egenAktivitetData: EgenAktivitetData,
    ): AktivitetsEndring
}
