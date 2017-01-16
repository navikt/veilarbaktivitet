package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.modig.security.ws.SystemSAMLOutInterceptor;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;

@Configuration
public class AktoerContext {

    @Bean
    public Aktoer_v2PortType aktoerV2() {
        return aktoerPortType()
                .withOutInterceptor(new SystemSAMLOutInterceptor())
                .build();
    }

    @Bean
    public AktoerConsumer aktoerService() {
        return new AktoerConsumer();
    }

    private CXFClient<Aktoer_v2PortType> aktoerPortType() {
        return new CXFClient<>(Aktoer_v2PortType.class)
                .address(getProperty("aktoer.endpoint.url"));
    }
}
