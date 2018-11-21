package no.nav.fo.veilarbaktivitet.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;

@Configuration
public class ArenaServiceConfig {

    public static CXFClient<TiltakOgAktivitetV1> tiltakOgAktivitetV1Client() {
        return new CXFClient<>(TiltakOgAktivitetV1.class)
                .address(getProperty("tiltakOgAktivitet.endpoint.url"))
                .withMetrics()
                .withOutInterceptor(new LoggingOutInterceptor());
    }
}