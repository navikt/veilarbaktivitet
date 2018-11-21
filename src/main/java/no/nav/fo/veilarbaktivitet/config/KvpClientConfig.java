package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

@Configuration
public class KvpClientConfig {

    @Value("${veilarboppfolging.api.url}")
    private String VEILARBOPPFOLGING_API_URL;

    @Bean
    public KvpClient kvpClient() {
        Client client = RestUtils.createClient();
        return new KvpClient(VEILARBOPPFOLGING_API_URL, client);
    }
}
