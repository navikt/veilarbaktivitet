package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.abac.Pep;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.featuretoggle.UnleashClient;
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

@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(
            VeilarbarenaHelsesjekk veilarbarenaHelsesjekk,
            AktorOppslagClient aktorOppslagClient,
            Pep pep,
            DatabaseHelsesjekk databaseHelsesjekk,
            UnleashClient unleashClient,
            KafkaHelsesjekk kafkaHelsesjekk,
            MeterRegistry meterRegistry
    ) {
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("Veilarbarena", false, veilarbarenaHelsesjekk),
                new SelfTestCheck("Aktorregister", true, aktorOppslagClient),
                new SelfTestCheck("ABAC", true, pep.getAbacClient()),
                new SelfTestCheck("DatabaseHelsesjekk", true, databaseHelsesjekk),
                new SelfTestCheck("Unleash", false, unleashClient),
                new SelfTestCheck("Kafka", false, kafkaHelsesjekk)
        );
        var checks = new SelfTestChecks(selfTestChecks);
        new SelfTestMeterBinder(checks).bindTo(meterRegistry);
        return checks;
    }
}
