package no.nav.veilarbaktivitet.config.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Component
public class KafkaHelsesjekk implements HealthCheck {

    private final AtomicBoolean isHealty = new AtomicBoolean(true);
    private String feilmelding = "";


    public void setIsHealty(boolean isHealty, String feilmelding) {
        this.isHealty.set(isHealty);
        this.feilmelding = feilmelding;
    }

    @Override
    public HealthCheckResult checkHealth() {
        if (isHealty.get()) {
            return HealthCheckResult.healthy();
        }
        return HealthCheckResult.unhealthy(feilmelding);
    }
}
