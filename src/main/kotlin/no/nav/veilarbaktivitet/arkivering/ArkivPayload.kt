package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett

typealias ArkivAktivitetStatus = String

data class ArkivPayload(
    val metadata: Metadata,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>
)

data class Metadata(
    val navn: String,
    val fnr: String,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?
)

data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
    val meldinger: List<Melding>,
    val etiketter: List<ArkivEtikett>
)

data class ArkivDialogtråd(
    val overskrift: String,
    val meldinger: List<Melding>,
    val egenskaper: List<String>
)

data class Melding(
    val avsender: String,
    val sendt: String,
    val lest: Boolean,
    val viktig: Boolean,
    val tekst: String,
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


