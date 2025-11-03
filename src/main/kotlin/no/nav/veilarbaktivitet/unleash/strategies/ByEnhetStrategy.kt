package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.client.msgraph.AdGroupFilter
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
@RequiredArgsConstructor
@Slf4j
class ByEnhetStrategy(
    private val msGraphClient: MsGraphClient,
    private val azureAdMachineToMachineTokenClient: AzureAdMachineToMachineTokenClient,
    @param:Value("\${msgraph.scope}")
    val msGraphScope: String
) : Strategy {
    private val log = LoggerFactory.getLogger(javaClass)
    private val PARAM = "valgtEnhet"
    val ENHET_PREFIKS = "0000-GA-ENHET_"

    override fun getName() =  "byEnhet"
    fun isEnabled(parameters: Map<String, String>) = false

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
        val accessToken = azureAdMachineToMachineTokenClient.createMachineToMachineToken(msGraphScope)
        val groups = msGraphClient.hentAdGroupsForUser(accessToken, navIdent, AdGroupFilter.ENHET)
        return groups.mapNotNull { it.displayName.split(ENHET_PREFIKS)[1] }
    }

    private fun erNavIdent(verdi: String): Boolean {
        return verdi.matches("\\w\\d{6}".toRegex())
    }
}
