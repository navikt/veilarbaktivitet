package no.nav.fo.veilarbaktivitet.config;

import no.nav.fo.veilarbaktivitet.client.KvpClient;
import no.nav.fo.veilarbaktivitet.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class KvpClientConfig {

    @Bean
    public KvpClient kvpClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        return new KvpClient(RestClient.build(httpServletRequestProvider, System.getProperty("kvp.endpoint.url")));
    }
}
