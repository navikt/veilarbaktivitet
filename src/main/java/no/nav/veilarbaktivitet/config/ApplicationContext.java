package no.nav.veilarbaktivitet.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.unleash.UnleashConfig;
import no.nav.veilarbaktivitet.unleash.strategies.ByEnhetStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.NaisUtils.getCredentials;

@Configuration
@Profile("!test")
@EnableConfigurationProperties({EnvironmentProperties.class, no.nav.veilarbaktivitet.unleash.UnleashConfig.class})
public class ApplicationContext {
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String VEILARB_KASSERING_IDENTER_PROPERTY = "VEILARB_KASSERING_IDENTER";

    @Bean
    public Unleash unleash(UnleashConfig config, ByEnhetStrategy byEnhetStrategy) {
        return new DefaultUnleash(config.toUnleashConfig(), byEnhetStrategy);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return getCredentials("service_user");
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient();
    }
}
