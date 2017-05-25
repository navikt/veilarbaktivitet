package no.nav.fo.veilarbaktivitet.config;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArenaServiceRestConfig {

    @Bean
    public TiltakOgAktivitetV1 tiltakOgAktivitetV1() {
        return ArenaServiceConfig.tiltakOgAktivitetV1Client()
                .configureStsForOnBehalfOfWithJWT()
                .build();
    }
}
