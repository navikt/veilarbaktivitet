package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarbaktivitet.helsesjekk.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(ArenaServiceHelsesjekk arenaServiceHelsesjekk,
                                         AktorregisterClient aktorregisterClient,
                                         Pep pep,
                                         DatabaseHelsesjekk databaseHelsesjekk,
                                         KafkaHelsesjekk kafkaHelsesjekk,
                                         UnleashHelsesjekk unleashHelsesjekk) {
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("TiltakOgAktivitetV1", false, arenaServiceHelsesjekk),
                new SelfTestCheck("Aktorregister", true, aktorregisterClient),
                new SelfTestCheck("ABAC", true, pep.getAbacClient()),
                new SelfTestCheck("DatabaseHelsesjekk", true, databaseHelsesjekk),
                new SelfTestCheck("KafkaHelsesjekk", false, kafkaHelsesjekk),
                new SelfTestCheck("Unleash", false, unleashHelsesjekk)
        );

        return new SelfTestChecks(selfTestChecks);
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }
}
