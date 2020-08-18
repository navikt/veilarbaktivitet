package no.nav.veilarbaktivitet.config;

import no.nav.veilarbaktivitet.client.KvpClient;
import no.nav.veilarbaktivitet.mock.KvpClientMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientTestConfig {

    @Bean
    public KvpClient kvpClient() {
        return new KvpClientMock();
    }
}
