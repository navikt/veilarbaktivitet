package no.nav.fo.veilarbaktivitet.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.springframework.stereotype.Component;

import static java.lang.System.getProperty;
import static no.nav.fo.veilarbaktivitet.config.ArenaServiceConfig.tiltakOgAktivitetV1Client;

@Component
public class ArenaServiceHelsesjekk implements Helsesjekk {
    @Override
    public void helsesjekk() {
        final TiltakOgAktivitetV1 tiltakOgAktivitetV1 = tiltakOgAktivitetV1Client()
                .configureStsForSystemUserInFSS()
                .build();

        tiltakOgAktivitetV1.ping();
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        String tiltakUri = getProperty("tiltakOgAktivitet.endpoint.url");

        return new HelsesjekkMetadata(
                "TILTAKOGAKTIVITET_V1",
                tiltakUri,
                "Ping av tjeneste for å hente tiltak og aktiviteter.",
                false
        );
    }
}
