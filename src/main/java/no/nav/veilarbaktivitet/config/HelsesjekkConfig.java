package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarbaktivitet.arena.VeilarbarenaHelsesjekk;
import no.nav.veilarbaktivitet.config.database.DatabaseHelsesjekk;
import no.nav.veilarbaktivitet.config.kafka.KafkaHelsesjekk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(
            VeilarbarenaHelsesjekk veilarbarenaHelsesjekk,
            AktorOppslagClient aktorOppslagClient,
            DatabaseHelsesjekk databaseHelsesjekk,
            KafkaHelsesjekk kafkaHelsesjekk,
            MeterRegistry meterRegistry
    ) {  //TODO legg til poao tilgang.
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("Veilarbarena", false, veilarbarenaHelsesjekk),
                new SelfTestCheck("Aktorregister", true, aktorOppslagClient),
                new SelfTestCheck("DatabaseHelsesjekk", true, databaseHelsesjekk),
                new SelfTestCheck("Kafka", false, kafkaHelsesjekk)
        );
        var checks = new SelfTestChecks(selfTestChecks);
        try {
            new SelfTestMeterBinder(checks).bindTo(meterRegistry);
        } catch (Exception e) {
            log.error("Helsesjekk-metrikker feilet", e);
        }
        return checks;
    }
}
