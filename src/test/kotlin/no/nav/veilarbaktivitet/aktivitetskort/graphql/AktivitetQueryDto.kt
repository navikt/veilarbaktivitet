package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.veilarbaktivitet.aktivitet.Historikk

data class AktivitetQueryDto(
    val portefoljeKafkaOffsetAiven: Long? = null,
    val historikk: Historikk? = null,
)