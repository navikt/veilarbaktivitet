package no.nav.fo.veilarbaktivitet.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Configuration;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class ArenaServiceConfig {

    public static CXFClient<TiltakOgAktivitetV1> tiltakOgAktivitetV1Client() {
        return new CXFClient<>(TiltakOgAktivitetV1.class)
                .address(getRequiredProperty(VIRKSOMHET_TILTAKOGAKTIVITET_V1_ENDPOINTURL_PROPERTY))
                .withMetrics()
                .withOutInterceptor(new LoggingOutInterceptor());
    }
}
