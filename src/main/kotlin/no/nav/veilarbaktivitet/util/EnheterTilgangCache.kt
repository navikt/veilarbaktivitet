package no.nav.veilarbaktivitet.util

import no.nav.common.types.identer.EnhetId

class EnheterTilgangCache(private val harTilgangTilEnhet: (EnhetId) -> Boolean) {
    val kontrollerteEnheter: MutableMap<String, Boolean> = mutableMapOf()

    fun harTilgang(enhetId: String): Boolean {
        return kontrollerteEnheter.getOrElse(enhetId) {
            val tilgang = harTilgangTilEnhet(EnhetId.of(enhetId))
            kontrollerteEnheter.put(enhetId, tilgang)
            tilgang
        }
    }
}