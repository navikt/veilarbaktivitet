package no.nav.veilarbaktivitet.person

import no.nav.common.client.pdl.PdlClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.poao.dab.spring_auth.IAuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PdlClientConfig(val authService: IAuthService) {

    @Bean
    open fun pdlClient(pdlUrl: String?, tokenClient: AzureAdOnBehalfOfTokenClient, pdlTokenscope: String): PdlClient {
        return PdlClientImpl(pdlUrl, {tokenClient.exchangeOnBehalfOfToken(pdlTokenscope, authService.getInnloggetBrukerToken())}, "B579" )
    }
}