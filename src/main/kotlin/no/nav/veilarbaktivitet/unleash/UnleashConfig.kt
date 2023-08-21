package no.nav.veilarbaktivitet.unleash

import io.getunleash.util.UnleashConfig
import no.nav.common.utils.EnvironmentUtils
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "unleash")
data class UnleashConfig(
    val appName: String,
    val url: String,
    val token: String,
    val instanceId: String,
) {
    fun toUnleashConfig(): UnleashConfig {
        val environment = if (EnvironmentUtils.isProduction().orElse(false)) "production" else "development"

        return UnleashConfig.builder()
            .appName(appName)
            .instanceId(instanceId)
            .unleashAPI("$url/api")
            .apiKey(token)
            .environment(environment)
            .build()
    }
}
