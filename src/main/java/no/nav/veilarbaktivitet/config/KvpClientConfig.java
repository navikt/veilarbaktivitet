package no.nav.veilarbaktivitet.config;

import no.nav.sbl.rest.RestUtils;
import no.nav.veilarbaktivitet.client.KvpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Configuration
public class KvpClientConfig {

    @Bean
    public KvpClient kvpClient() {
        Client client = RestUtils.createClient();
        return new KvpClient(getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY), client);
    }
}
