package no.nav.veilarbaktivitet.aktivitet.domain

import java.time.ZonedDateTime
import java.util.UUID


data class Motedeltaker(
    val rolle: MotedeltakerRolle,
    val navn: String,
    val opprettet: ZonedDateTime? = null,
    val deltakerId: UUID? = null,
) {
    fun viskUtNavn(): Motedeltaker {
        return this.copy(navn = "")
    }
}

enum class MotedeltakerRolle(val rolle: String) {
    ARBEIDSGIVER("Arbeidsgiver"),
    MENTOR_FADDER("Mentor/Fadder"),
    VEILEDER("Veileder"),
    TOLK("Tolk"),
    BRUKER("Bruker"),
    ANNET("Annet");
}
