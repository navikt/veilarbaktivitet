package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;

@Configuration
public class KvpClientConfig {

    private static final String KVP_ENDPOINT_URL = "kvp.endpoint.url";
    private static final String KVP_CONNECT_TIMEOUT = "kvp.connect.timeout.ms";
    private static final String KVP_READ_TIMEOUT = "kvp.read.timeout.ms";

    @Bean
    public KvpClient kvpClient() {
        RestUtils.RestConfig.RestConfigBuilder configBuilder = RestUtils.RestConfig.builder();
        configBuilder.readTimeout(Integer.parseInt(System.getProperty(KVP_READ_TIMEOUT, "1000")));
        configBuilder.connectTimeout(Integer.parseInt(System.getProperty(KVP_CONNECT_TIMEOUT, "1000")));
        RestUtils.RestConfig config = configBuilder.build();
        Client client = RestUtils.createClient(config);
        return new KvpClient(System.getProperty(KVP_ENDPOINT_URL), client);
    }
}
