package no.nav.veilarbaktivitet.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient;
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClientImplementation;
import no.nav.veilarbaktivitet.unleash.UnleashConfig;
import no.nav.veilarbaktivitet.unleash.strategies.ByEnhetStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration
@Profile("!test")
@EnableConfigurationProperties({EnvironmentProperties.class, no.nav.veilarbaktivitet.unleash.UnleashConfig.class})
public class ApplicationContext {
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";

    @Bean
    public Unleash unleash(UnleashConfig config, ByEnhetStrategy byEnhetStrategy) {
        return new DefaultUnleash(config.toUnleashConfig(), byEnhetStrategy);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public BigQueryClient bigQueryClient(BigQueryClientImplementation bigQueryClient) {
        return bigQueryClient;
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClient() {
            @Override
            public void report(Event event) {
                // TODO: Fix metrikker
            }

            @Override
            public void report(String s, Map<String, Object> map, Map<String, String> map1, long l) {
                // TODO: Fix metrikker
            }
        };
    }
}
