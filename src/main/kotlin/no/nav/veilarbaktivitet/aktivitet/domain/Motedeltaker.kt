package no.nav.veilarbaktivitet.aktivitet.domain

import java.time.ZonedDateTime


data class Motedeltaker(
    val rolle: MotedeltakerRolle,
    val navn: String,
    val opprettet: ZonedDateTime = ZonedDateTime.now()
)

enum class MotedeltakerRolle(val rolle: String) {
    ARBEIDSGIVER("Arbeidsgiver"),
    MENTOR_FADDER("Mentor/Fadder"),
    VEILEDER("Veileder"),
    TOLK("Tolk"),
    BRUKER("Bruker"),
    ANNET("Annet");
}
