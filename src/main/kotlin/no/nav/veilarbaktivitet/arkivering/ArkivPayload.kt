package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import java.util.*

typealias ArkivAktivitetStatus = String

data class ArkivPayload(
    val navn: String,
    val fnr: String,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val sakId: Long,
    val fagsaksystem: String,
    val tema: String,
    val oppfølgingsperiodeId: UUID,
    val journalførendeEnhet: String,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>,
    val mål: String?,
)

data class ForhåndsvisningPayload(
    val navn: String,
    val fnr: String,
    val oppfølgingsperiodeStart: String,
    val oppfølgingsperiodeSlutt: String?,
    val oppfølgingsperiodeId: UUID,
    val aktiviteter: Map<ArkivAktivitetStatus, List<ArkivAktivitet>>,
    val dialogtråder: List<ArkivDialogtråd>,
    val mål: String?,
)

data class ArkivAktivitet(
    val tittel: String,
    val type: String,
    val status: String,
    val detaljer: List<Detalj>,
    val meldinger: List<Melding>,
    val etiketter: List<ArkivEtikett>,
    val eksterneHandlinger: List<EksternHandling>,
    val historikk: AktivitetHistorikk
)

data class AktivitetHistorikk(
    val endringer: List<AktivitetEndring>
)

data class AktivitetEndring(
    val formattertTidspunkt: String,
    val beskrivelse: String
)


data class ArkivDialogtråd(
    val overskrift: String,
    val meldinger: List<Melding>,
    val egenskaper: List<String>,
    val indeksSistMeldingLestAvBruker: Int?,
    val tidspunktSistLestAvBruker: String,
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
    LENKE,
    EKSTERN_AKTIVITET_HANDLING
}

data class Detalj(
    val stil: Stil,
    val tittel: String,
    val tekst: String?
)

data class EksternHandling(
    val tekst: String,
    val subtekst: String?,
    val url: String
)

//data class Tag(
//    val tekst: String,
//    val type: String
//)


