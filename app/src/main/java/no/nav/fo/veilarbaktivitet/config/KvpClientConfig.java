package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;

@Configuration
public class KvpClientConfig {

    private static final String KVP_ENDPOINT_URL = "kvp.endpoint.url";
    private static final String KVP_CONNECT_TIMEOUT = "kvp.connect.timeout.ms";
    private static final String KVP_READ_TIMEOUT = "kvp.read.timeout.ms";

    @Bean
    public KvpClient kvpClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        RestUtils.RestConfig.RestConfigBuilder configBuilder = RestUtils.RestConfig.builder();
        configBuilder.readTimeout(Integer.parseInt(System.getProperty(KVP_READ_TIMEOUT)));
        configBuilder.connectTimeout(Integer.parseInt(System.getProperty(KVP_CONNECT_TIMEOUT)));
        RestUtils.RestConfig config = configBuilder.build();
        Client client = RestUtils.createClient(config);
        return new KvpClient(System.getProperty(KVP_ENDPOINT_URL), client);
    }
}
