package no.nav.fo.veilarbaktivitet.config;

import no.nav.modig.security.ws.UserSAMLOutInterceptor;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.System.getProperty;

@Configuration

public class ArenaServiceWSConfig {

    @Bean
    public TiltakOgAktivitetV1 tiltakOgAktivitetV1() {
        return ArenaServiceConfig.tiltakOgAktivitetV1Client()
                .withOutInterceptor(new UserSAMLOutInterceptor())
                .build();
    }

}