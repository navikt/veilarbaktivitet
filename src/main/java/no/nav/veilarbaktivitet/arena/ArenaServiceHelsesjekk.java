package no.nav.veilarbaktivitet.arena;

import no.nav.common.cxf.CXFClient;
import no.nav.common.cxf.StsConfig;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.utils.Credentials;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.veilarbaktivitet.config.ApplicationContext;
import no.nav.veilarbaktivitet.config.EnvironmentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY;

@Component
@Profile("!dev")
public class ArenaServiceHelsesjekk implements HealthCheck {

    private final TiltakOgAktivitetV1 tiltakOgAktivitetV1;

    @Autowired
    public ArenaServiceHelsesjekk(EnvironmentProperties properties, Credentials credentials) {
        StsConfig stsConfig = StsConfig.builder()
                .url(properties.getCxfStsUrl())
                .username(credentials.username)
                .password(credentials.password)
                .build();

        this.tiltakOgAktivitetV1 = new CXFClient<>(TiltakOgAktivitetV1.class)
                .address(getRequiredProperty(ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY))
                .configureStsForSystemUser(stsConfig)
                .build();

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
