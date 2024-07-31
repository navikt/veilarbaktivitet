package no.nav.veilarbaktivitet.config

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.builder.TokenXTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.TokenXOnBehalfOfTokenClient
import no.nav.poao.dab.spring_a2_annotations.EnableAuthorization
import no.nav.poao.dab.spring_auth.AuthService
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.veilarbaktivitet.person.PersonService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableAuthorization
open class AuthorizationConfig {
    @Bean
    open fun poaoTilgangClient(
        @Value("\${app.env.poao_tilgang.url}") baseUrl: String,
        tokenProvider: AzureAdMachineToMachineTokenClient,
        @Value("\${app.env.poao_tilgang.scope}") scope: String
    ): PoaoTilgangClient {
        val poaoTilgangHttpClient = PoaoTilgangHttpClient(baseUrl, { tokenProvider.createMachineToMachineToken(scope) })
        return PoaoTilgangCachedClient(poaoTilgangHttpClient)
    }

    @Bean
    open fun authService(
        authContextHolder: AuthContextHolder,
        poaoTilgangClient: PoaoTilgangClient,
        personService: PersonService
    ): IAuthService {
        return AuthService(authContextHolder, poaoTilgangClient, personService, "veilarbaktivitet")
    }

    @Bean
    @Profile("!test")
    open fun machineToMachineTokenClient(): AzureAdMachineToMachineTokenClient {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient()
    }

    @Bean
    @Profile("!test")
    open fun onBehalfOfTokenClient(): AzureAdOnBehalfOfTokenClient {
        return AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildOnBehalfOfTokenClient()
    }

    @Bean
    @Profile("!test")
    open fun tokenXOnBehalfOfTokenClient(): TokenXOnBehalfOfTokenClient {
        return TokenXTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildOnBehalfOfTokenClient()
    }
}
