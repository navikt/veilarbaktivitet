package no.nav.veilarbaktivitet.brukernotifikasjon

import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Varseltype

//@Data
data class OpprettVarselDto (
    var varselId: String,
    var ident: String,
    var tekster: List<Tekst>,
    var link: String,
    var type: Varseltype,
    var eksternVarsling: EksternVarslingBestilling,
    var produsent: Produsent
)

class Tekst(
    val tekst: String,
    val spraakkode: String,
    val default: Boolean
)
