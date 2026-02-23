package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
@RequiredArgsConstructor
@Slf4j
class ByEnhetStrategy(
    private val poaoTilgangClient: PoaoTilgangClient
) : Strategy {
    private val log = LoggerFactory.getLogger(javaClass)
    private val PARAM = "valgtEnhet"
    val ENHET_PREFIKS = "0000-GA-ENHET_"

    override fun getName() =  "byEnhet"
    fun isEnabled(parameters: Map<String, String>) = false

    override fun isEnabled(parameters: Map<String, String>, context: UnleashContext): Boolean {
        // NavAnsattAzure id fra oid claim, se FeatureController
        return context.userId
            .flatMap { userId ->
                Optional.ofNullable(parameters[PARAM])
                    .map { enheter -> enheter.split(",\\s?".toRegex()) }
                    .map { enabledEnheter -> enabledEnheter.intersect(brukersEnheter(userId).toSet()).isNotEmpty() }
            }.orElse(false)
    }

    private fun brukersEnheter(navAnsattAzureId: String): List<String> {
        if (!erNavAzureId(navAnsattAzureId)) {
            log.warn("Fikk ident som ikke er en NAVident. Om man ser mye av denne feilen bør man utforske hvorfor.")
            return emptyList()
        }
        return hentEnheter(navAnsattAzureId)
    }

    private fun hentEnheter(navAnsattAzureId: String): List<String> {
        return poaoTilgangClient.hentAdGrupper(UUID.fromString(navAnsattAzureId))
            .map { adGrupper -> adGrupper.mapNotNull { it.navn.split(ENHET_PREFIKS).getOrNull(1) } }
            .getOrThrow()
    }

    private fun erNavAzureId(verdi: String): Boolean {
        return verdi.length == 36
    }
}
