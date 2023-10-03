package no.nav.veilarbaktivitet.aktivitetskort.service

enum class UpsertActionResult {
    OPPDATER,
    OPPRETT,
    KASSER,
    FUNKSJONELL_FEIL,
    IGNORER
}
