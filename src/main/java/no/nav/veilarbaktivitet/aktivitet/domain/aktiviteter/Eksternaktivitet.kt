package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.internapi.model.AktivitetType
import java.util.*

class Eksternaktivitet {

    class Opprett(
        val funksjonellId: UUID,
        val avtaltMedNav: Boolean,
        override val opprettFelter: AktivitetBareOpprettFelter,
        override val muterbareFelter: AktivitetMuterbareFelter,
        override val sporing: SporingsData,
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
    ): AktivitetsEndring {
        override val aktivitetType = AktivitetTypeData.EKSTERNAKTIVITET
    }
}
