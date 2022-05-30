package no.nav.veilarbaktivitet.person;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.AktorregisterHttpClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.veilarbaktivitet.config.EnvironmentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.common.utils.UrlUtils.createDevInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createProdInternalIngressUrl;
import static no.nav.veilarbaktivitet.config.ApplicationContext.APPLICATION_NAME;

@Configuration
public class AktorClientConfig {
    @Bean
    public AktorOppslagClient aktorClient(SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                isProduction().orElse(false)
                        ? createProdInternalIngressUrl("pdl-api")
                        : createDevInternalIngressUrl("pdl-api"),
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken
        );

        return new CachedAktorOppslagClient(aktorOppslagClient);
    }
}
