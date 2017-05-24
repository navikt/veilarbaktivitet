package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public ArenaAktivitetConsumer arenaAktivitetService() {
        return new ArenaAktivitetConsumer();
    }

}