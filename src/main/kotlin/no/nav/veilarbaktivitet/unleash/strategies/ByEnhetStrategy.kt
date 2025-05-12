package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.types.identer.NavIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
@RequiredArgsConstructor
@Slf4j
class ByEnhetStrategy(
    private val axsysClient: AxsysClient
) : Strategy {
    private val log = LoggerFactory.getLogger(javaClass)
    private val PARAM = "valgtEnhet"
    private val TEMA_OPPFOLGING = "OPP"

    override fun getName(): String {
        return "byEnhet"
    }

    fun isEnabled(parameters: Map<String, String>): Boolean {
        return false
    }

    override fun isEnabled(parameters: Map<String, String>, context: UnleashContext): Boolean {
        return context.userId
            .flatMap { userId ->
                Optional.ofNullable(parameters[PARAM])
                    .map { enheter -> enheter.split(",\\s?".toRegex()) }
                    .map { enabledEnheter -> enabledEnheter.intersect(brukersEnheter(userId).toSet()).isNotEmpty() }
            }.orElse(false)
    }

    private fun brukersEnheter(navIdent: String): List<String> {
        if (!erNavIdent(navIdent)) {
            log.warn("Fikk ident som ikke er en NAVident. Om man ser mye av denne feilen bør man utforske hvorfor.")
            return emptyList()
        }
        return hentEnheter(navIdent)
    }

    private fun hentEnheter(navIdent: String): List<String> {
        return axsysClient.hentTilganger(NavIdent(navIdent))
                .filter { it.temaer.contains(TEMA_OPPFOLGING) }
                .map { it.enhetId.get() }
    }

    private fun erNavIdent(verdi: String): Boolean {
        return verdi.matches("\\w\\d{6}".toRegex())
    }
}
