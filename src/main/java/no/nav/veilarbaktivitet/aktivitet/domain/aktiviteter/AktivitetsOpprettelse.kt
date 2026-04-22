package no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter

sealed interface AktivitetsOpprettelse {
    val opprettFelter: AktivitetBareOpprettFelter
    val muterbareFelter: AktivitetMuterbareFelter
    val sporing: SporingsData
}