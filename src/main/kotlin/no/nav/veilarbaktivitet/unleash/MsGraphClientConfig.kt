package no.nav.veilarbaktivitet.unleash

import no.nav.common.client.msgraph.CachedMsGraphClient
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.client.msgraph.MsGraphHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MsGraphClientConfig(
    @Value("\${msgraph.url}")
    private val msgraphUrl: String
) {

    @Bean
    fun msGraphClient(): MsGraphClient {
        return CachedMsGraphClient(MsGraphHttpClient(msgraphUrl))
    }

}