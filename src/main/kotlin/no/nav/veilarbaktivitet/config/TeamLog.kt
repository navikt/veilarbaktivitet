package no.nav.veilarbaktivitet.config;

import org.slf4j.LoggerFactory;

object TeamLog {
    @JvmField
    val teamLog = LoggerFactory.getLogger("team-logs-logger") ?: throw IllegalStateException("Klarte ikke å instansiere Team Logs logger.")
}
