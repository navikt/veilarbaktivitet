package no.nav.fo.veilarbaktivitet.config;

import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@Configuration
public class ArenaServiceConfig {

    public static CXFClient<TiltakOgAktivitetV1> tiltakOgAktivitetV1Client() {
        return new CXFClient<>(TiltakOgAktivitetV1.class)
                .address(getProperty("tiltakOgAktivitet.endpoint.url"))
                .withOutInterceptor(new LoggingOutInterceptor());
    }

    @Bean
    Pingable tiltakOgAktivitetPing() {
        return () -> {
            final TiltakOgAktivitetV1 tiltakOgAktivitetV1 = tiltakOgAktivitetV1Client()
                    .configureStsForSystemUserInFSS()
                    .build();
            try {
                tiltakOgAktivitetV1.ping();
                return lyktes("TILTAKOGAKTIVITET_V1");
            } catch (Exception e) {
                return feilet("TILTAKOGAKTIVITET_V1", e);
            }
        };
    }
}