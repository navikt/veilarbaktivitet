package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.config.OboTokenTidtaker
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PdlClientConfig(val authService: IAuthService) {

    @Value("\${pdl.url}")
    private val pdlUrl: String? = null

    @Value("\${pdl.scope}")
    private val pdlTokenscope: String? = null

    @Value("\${pdl.scope.tokenx}")
    private val pdlTokenscopeTokenX: String? = null

    @Bean
    open fun pdlClient(
        azureTokenClient: AzureAdOnBehalfOfTokenClient,
        tokenXTokenClient: TokenXOnBehalfOfTokenClient,
        oboTokenTidtaker: OboTokenTidtaker,
    ): PdlClient {
        val tokenClientSupplier = {
            val (tokenClient, tokenScope, leverandor) = when (authService.erInternBruker()) {
                true -> Triple(azureTokenClient, pdlTokenscope, "azure")
                false -> Triple(tokenXTokenClient, pdlTokenscopeTokenX, "tokenx")
            }
            oboTokenTidtaker.taTidPaaHentingAvToken(tilTjeneste = "pdl", leverandor = leverandor) {
                tokenClient.exchangeOnBehalfOfToken(tokenScope, authService.getInnloggetBrukerToken())
            }
        }
        return PdlClientImpl(pdlUrl, tokenClientSupplier, "B579")
    }
}
