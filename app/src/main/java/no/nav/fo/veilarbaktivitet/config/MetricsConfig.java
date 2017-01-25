package no.nav.fo.veilarbaktivitet.config;

import no.nav.metrics.aspects.TimerAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    @Bean
    public TimerAspect timerAspect() {
        return new TimerAspect();
    }
}
