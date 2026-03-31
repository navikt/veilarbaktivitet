package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData

class Eksternaktivitet {

    class Opprett(
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sproring: SporingsData,
        val eksternAktivitetData: EksternAktivitetData,
    ): AktivitetsOpprettelse

    data class Endre(
        override val id: Long,
        override val versjon: Long,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
        val eksternAktivitetData: EksternAktivitetData,
        val erAvtalt: Boolean,
        val status: AktivitetStatus
    ): AktivitetsEndring
}
