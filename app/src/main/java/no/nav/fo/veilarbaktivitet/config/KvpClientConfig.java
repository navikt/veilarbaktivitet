package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.fo.veilarbaktivitet.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class KvpClientConfig {

    private static final String KVP_ENDPOINT_URL = "kvp.endpoint.url";
    private static final String KVP_CONNECT_TIMEOUT = "kvp.connect.timeout.ms";
    private static final String KVP_READ_TIMEOUT = "kvp.read.timeout.ms";

    @Bean
    public KvpClient kvpClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new KvpClient(RestClient.build(
                httpServletRequestProvider,
                System.getProperty(KVP_ENDPOINT_URL),
                Long.decode(System.getProperty(KVP_CONNECT_TIMEOUT)),
                Long.decode(System.getProperty(KVP_READ_TIMEOUT))
                ));
    }
}
