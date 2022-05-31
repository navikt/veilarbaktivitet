package no.nav.veilarbaktivitet.person;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.sts.SystemUserTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.common.utils.UrlUtils.createDevInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createProdInternalIngressUrl;

@Configuration
public class AktorClientConfig {


    @Bean
    @Profile("!dev")
    public String pdlUrl() {
        return isProduction().orElse(false)
                ? createProdInternalIngressUrl("pdl-api")
                : createDevInternalIngressUrl("pdl-api");
    }

    @Bean
    public AktorOppslagClient aktorClient(String pdlUrl, SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                pdlUrl,
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken
        );

        return new CachedAktorOppslagClient(aktorOppslagClient);
    }
}
