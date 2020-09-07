package no.nav.veilarbaktivitet.helsesjekk;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

@Component
public class UnleashHelsesjekk implements HealthCheck {

    private final UnleashService unleash;

    public UnleashHelsesjekk(UnleashService unleash) {
        this.unleash = unleash;
    }

    @Override
    public HealthCheckResult checkHealth() {
         return unleash.checkHealth();
    }
}
