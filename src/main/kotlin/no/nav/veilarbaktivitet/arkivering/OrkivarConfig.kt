package no.nav.veilarbaktivitet.arkivering

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "orkivar")
data class OrkivarConfig(
    val url: String,
)