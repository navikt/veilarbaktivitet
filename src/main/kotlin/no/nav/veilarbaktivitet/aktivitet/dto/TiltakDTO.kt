package no.nav.veilarbaktivitet.aktivitet.dto

data class TiltakDTO(
    val tiltaksnavn: String,
    val arrangornavn: String,
    val deltakelseStatus: String,
    val dagerPerUke: Int,
    val deltakelsesprosent: Int
)
