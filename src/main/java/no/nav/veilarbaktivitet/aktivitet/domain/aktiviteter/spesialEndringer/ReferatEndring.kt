package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer

import no.nav.veilarbaktivitet.aktivitet.domain.MoteData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData

data class ReferatEndring(
    val id: Long,
    val versjon: Long,
    val sporingsData: SporingsData,
    val moteData: MoteData
)
