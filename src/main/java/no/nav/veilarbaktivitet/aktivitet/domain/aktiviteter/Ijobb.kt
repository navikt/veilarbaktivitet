package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.IJobbAktivitetData

class Ijobb(
    baseData: AktivitetBaseData,
    val iJobbAktivitetData: IJobbAktivitetData,
) : LestAktivitet(baseData) {

    class Opprett(
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sproring: SporingsData,
        val iJobbAktivitetData: IJobbAktivitetData,
    ): AktivitetsOpprettelse

    class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val iJobbAktivitetData: IJobbAktivitetData,
    ): AktivitetsEndring
}
