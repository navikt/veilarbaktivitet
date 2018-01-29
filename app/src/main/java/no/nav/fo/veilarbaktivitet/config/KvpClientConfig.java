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

    @Value("${kvp.connect.timeout.ms:1000}")
    private int KVP_CONNECT_TIMEOUT;

    @Value("${kvp.read.timeout.ms:1000}")
    private int KVP_READ_TIMEOUT;

    @Bean
    public KvpClient kvpClient() {
        RestUtils.RestConfig.RestConfigBuilder configBuilder = RestUtils.RestConfig.builder();

        configBuilder.readTimeout(KVP_READ_TIMEOUT);
        configBuilder.connectTimeout(KVP_CONNECT_TIMEOUT);

        RestUtils.RestConfig config = configBuilder.build();
        Client client = RestUtils.createClient(config);

        return new KvpClient(VEILARBOPPFOLGING_API_URL, client);
    }
}
