package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.poao.dab.spring_auth.IAuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PdlClientConfig(val authService: IAuthService) {

    @Value("\${pdl.url}")
    private val pdlUrl: String? = null

    @Value("\${pdl.scope}")
    private val pdlTokenscope: String? = null

    @Bean
    open fun pdlClient(tokenClient: AzureAdOnBehalfOfTokenClient): PdlClient {
        return PdlClientImpl(pdlUrl, {tokenClient.exchangeOnBehalfOfToken(pdlTokenscope, authService.getInnloggetBrukerToken())}, "B579" )
    }
}
