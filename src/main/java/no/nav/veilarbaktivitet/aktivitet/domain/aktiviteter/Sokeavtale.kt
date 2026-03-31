package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.SokeAvtaleAktivitetData

class Sokeavtale(
    baseData: AktivitetBaseData,
    val sokeAvtaleAktivitetData: SokeAvtaleAktivitetData,
) : LestAktivitet(baseData) {

    class Opprett(
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sproring: SporingsData,
        val sokeAvtaleAktivitetData: SokeAvtaleAktivitetData,
    ): AktivitetsOpprettelse

    class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val sokeAvtaleAktivitetData: SokeAvtaleAktivitetData,
    ): AktivitetsEndring
}
