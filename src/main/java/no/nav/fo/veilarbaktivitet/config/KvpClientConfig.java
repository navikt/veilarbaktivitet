package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class KvpClientConfig {

    @Bean
    public KvpClient kvpClient() {
        Client client = RestUtils.createClient();
        return new KvpClient(getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY), client);
    }
}
