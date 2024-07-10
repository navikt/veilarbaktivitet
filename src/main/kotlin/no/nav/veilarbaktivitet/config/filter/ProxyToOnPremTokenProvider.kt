package no.nav.veilarbaktivitet.config.filter

import lombok.RequiredArgsConstructor
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.poao.dab.spring_auth.IAuthService
import org.springframework.stereotype.Service

@Service
@RequiredArgsConstructor
open class ProxyToOnPremTokenProvider(
    private val authService: IAuthService,
    private val azureAdOnBehalfOfTokenClient: AzureAdOnBehalfOfTokenClient,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val tokenXOnBehalfOfTokenClient: TokenXOnBehalfOfTokenClient
) {

    private val scope = String.format(
        "api://%s-gcp.dab.veilarbaktivitet/.default",
        if (EnvironmentUtils.isProduction().orElse(false)) "prod" else "dev"
    )

    open fun getProxyToken(): String =
        when {
            authService.erInternBruker() -> azureAdOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken())
            authService.erEksternBruker() -> tokenXOnBehalfOfTokenClient.exchangeOnBehalfOfToken(scope, authService.getInnloggetBrukerToken())
            authService.erSystemBruker() -> machineToMachineTokenClient.createMachineToMachineToken(scope)
            else -> throw RuntimeException("Klarte ikke å identifisere brukertype")
        }
}