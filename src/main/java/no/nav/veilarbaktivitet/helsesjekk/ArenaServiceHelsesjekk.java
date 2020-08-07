package no.nav.veilarbaktivitet.helsesjekk;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY;

@Component
public class ArenaServiceHelsesjekk implements HealthCheck {

    private final TiltakOgAktivitetV1 tiltakOgAktivitetV1;

    @Autowired
    public ArenaServiceHelsesjekk(TiltakOgAktivitetV1 tiltakOgAktivitetV1){
        this.tiltakOgAktivitetV1 = tiltakOgAktivitetV1;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            tiltakOgAktivitetV1.ping();
        } catch (Throwable t) {
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot Arena tiltak: " + getRequiredProperty(VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY), t);
        }
        return HealthCheckResult.healthy();
    }
}
