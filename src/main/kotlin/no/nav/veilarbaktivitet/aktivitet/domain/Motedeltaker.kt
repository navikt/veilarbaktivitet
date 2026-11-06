package no.nav.veilarbaktivitet.aktivitet.domain


data class Motedeltaker(
    val roller: List<MotedeltakerRolle>,
    val navn: String,
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
