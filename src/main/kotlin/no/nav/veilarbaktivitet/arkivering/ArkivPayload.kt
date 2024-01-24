package no.nav.veilarbaktivitet.arkivering

data class ArkivPayload(
    val metadata: Metadata,
    val aktiviteter: List<ArkivAktivitet>
)

data class Metadata(
    val navn: String,
    val fnr: String
)

data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
//    val tags: List<Tag>
)

enum class Stil {
    HEL_LINJE,
    HALV_LINJE,
    PARAGRAF,
    LENKE
}

data class Detalj(
    val stil: Stil,
    val tittel: String,
    val tekst: String?
)

//data class Tag(
//    val tekst: String,
//    val type: String
//)
