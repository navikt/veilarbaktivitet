package no.nav.fo.veilarbaktivitet.config;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import org.springframework.stereotype.Component;

import static no.nav.fo.veilarbaktivitet.ApplicationContext.VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ENDPOINTURL_PROPERTY;
import static no.nav.fo.veilarbaktivitet.config.ArenaServiceConfig.tiltakOgAktivitetV1Client;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Component
public class ArenaServiceHelsesjekk implements Helsesjekk {

    @Override
    public void helsesjekk() {
        final TiltakOgAktivitetV1 tiltakOgAktivitetV1 = tiltakOgAktivitetV1Client()
                .configureStsForSystemUser()
                .build();
        tiltakOgAktivitetV1.ping();
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(
                "TILTAKOGAKTIVITET_V1",
                getRequiredProperty(VIRKSOMHET_TILTAK_OG_AKTIVITET_V1_ENDPOINTURL_PROPERTY),
                "Ping av tjeneste for å hente tiltak og aktiviteter.",
                false
        );
    }
}
