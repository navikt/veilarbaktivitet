package no.nav.veilarbaktivitet.person;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.common.utils.UrlUtils.createDevInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createProdInternalIngressUrl;

@Configuration
public class AktorClientConfig {


    @Bean
    @Profile("!test")
    public String pdlUrl() {
        return isProduction().orElse(false)
                ? createProdInternalIngressUrl("pdl-api")
                : createDevInternalIngressUrl("pdl-api");
    }

    @Profile("!test")
    @Bean String pdlTokenscope() {
        return String.format("api://%s-fss.pdl.pdl-api/.default",
                isProduction().orElse(false) ? "prod" : "dev"
        );
    }

    @Bean
    public AktorOppslagClient aktorClient(String pdlUrl, MachineToMachineTokenClient tokenClient, String pdlTokenscope) {
        return new CachedAktorOppslagClient(new PdlAktorOppslagClient(
                pdlUrl,
                () -> tokenClient.createMachineToMachineToken(pdlTokenscope))
        );
    }
}
