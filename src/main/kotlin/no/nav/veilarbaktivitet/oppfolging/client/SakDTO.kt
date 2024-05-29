package no.nav.veilarbaktivitet.oppfolging.client

import java.util.*

data class SakDTO(
    val oppfolgingsperiodeId: UUID,
    val sakId: Long,
    val fagsaksystem: String,
    val tema: String
)
