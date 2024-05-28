package no.nav.veilarbaktivitet.avtalt_med_nav

import java.util.*

data class ForhaandsorienteringDTO(
    val id: String,
    val type: Type,
    val tekst: String,
    val lestDato: Date?,
)

data class ForhaandsorienteringInboundDTO(
    val type: Type,
    val tekst: String,
)
