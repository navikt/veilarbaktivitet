package no.nav.veilarbaktivitet.avtalt_med_nav

import no.nav.veilarbaktivitet.person.Person
import java.util.*

data class Forhaandsorientering(
    val id: String,
    val type: Type,
    val tekst: String,
    val lestDato: Date?,
    val aktorId: Person.AktorId,
    val arenaAktivitetId: String?,
    val aktivitetId: String,
    val aktivitetVersjon: String,
    val opprettetDato: Date,
    val opprettetAv: String,
    val varselId: String,
    val varselSkalStoppesDato: Date?,
    val varselStoppetDato: Date?,
) {
    fun toDTO() = ForhaandsorienteringDTO(id, type, tekst, lestDato)
}
