package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData

data class StatusEndring(
    val id: Long,
    val versjon: Long,
    val sporingsData: SporingsData,
    val status: AktivitetStatus,
    val avsluttetKommentar: String?,
)
