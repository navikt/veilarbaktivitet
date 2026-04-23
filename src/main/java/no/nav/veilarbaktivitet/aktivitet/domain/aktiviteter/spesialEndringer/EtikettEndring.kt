package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer

import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData

data class EtikettEndring(
    val id: Long,
    val versjon: Long,
    val sporingsData: SporingsData,
    val stillingsokEtikettData: StillingsoekEtikettData
)
