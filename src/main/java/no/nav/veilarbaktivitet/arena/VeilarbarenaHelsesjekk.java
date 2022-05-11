package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VeilarbarenaHelsesjekk implements HealthCheck {

    enum HealthStatus {
       OK,
       ERROR,
       UNAVAILABLE
    }
    private final VeilarbarenaClient veilarbarenaClient;

    @Override
    public HealthCheckResult checkHealth() {
        HealthStatus healthStatus = veilarbarenaClient.ping();
        return switch (healthStatus) {
            case OK -> HealthCheckResult.healthy();
            case ERROR -> HealthCheckResult.unhealthy("Feil fra veilarena selftest");
            case UNAVAILABLE -> HealthCheckResult.unhealthy("Kall mot veilarbarena feilet");
        };
    }
}
