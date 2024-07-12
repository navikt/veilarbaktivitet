package no.nav.veilarbaktivitet.person;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.token_client.client.MachineToMachineTokenClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AktorClientConfig {

    @Value("${pdl.url}")
    private String pdlUrl;
    @Value("${pdl.scope}")
    private String pdlTokenscope;

    @Bean
    public AktorOppslagClient aktorClient(MachineToMachineTokenClient tokenClient) {
        return new CachedAktorOppslagClient(new PdlAktorOppslagClient(
                pdlUrl,
                () -> tokenClient.createMachineToMachineToken(pdlTokenscope))
        );
    }
}
