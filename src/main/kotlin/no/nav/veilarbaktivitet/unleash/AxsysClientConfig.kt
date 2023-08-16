package no.nav.veilarbaktivitet.unleash

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.client.axsys.CachedAxsysClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbaktivitet.config.EnvironmentProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!dev")
open class AxsysClientConfig {

    private val axsysScope = "api://%s-fss.org.axsys/.default".format(
        if (EnvironmentUtils.isProduction().orElse(false)) "prod" else "dev"
    )

    private val axsysUrl = "http://axsys.org.svc.nais.local"

    @Bean
    open fun axsysClient(
        environmentProperties: EnvironmentProperties,
        azureAdMachineToMachineTokenClient: AzureAdMachineToMachineTokenClient
    ): AxsysClient {
        val axsysClient: AxsysClient = AxsysV2ClientImpl(axsysUrl) {
            azureAdMachineToMachineTokenClient.createMachineToMachineToken(axsysScope)
        }
        return CachedAxsysClient(axsysClient)
    }

}
