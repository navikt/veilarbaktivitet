package no.nav.veilarbaktivitet.arkivering

data class ArkivPayload(
    val metadata: Metadata
)

data class Metadata(
    val navn: String,
    val fnr: String
)
