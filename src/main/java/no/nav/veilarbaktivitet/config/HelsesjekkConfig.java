package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarbaktivitet.helsesjekk.ArenaServiceHelsesjekk;
import no.nav.veilarbaktivitet.helsesjekk.DatabaseHelsesjekk;
import no.nav.veilarbaktivitet.service.MoteSMSService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(
            ArenaServiceHelsesjekk arenaServiceHelsesjekk,
            AktorOppslagClient aktorOppslagClient,
            Pep pep,
            DatabaseHelsesjekk databaseHelsesjekk,
            UnleashClient unleashClient,
            MoteSMSService moteSMSService
    ) {
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("TiltakOgAktivitetV1", false, arenaServiceHelsesjekk),
                new SelfTestCheck("Aktorregister", true, aktorOppslagClient),
                new SelfTestCheck("ABAC", true, pep.getAbacClient()),
                new SelfTestCheck("DatabaseHelsesjekk", true, databaseHelsesjekk),
                new SelfTestCheck("Unleash", false, unleashClient),
                new SelfTestCheck("MoteServicemelding", false, moteSMSService)
        );

        return new SelfTestChecks(selfTestChecks);
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }
}
