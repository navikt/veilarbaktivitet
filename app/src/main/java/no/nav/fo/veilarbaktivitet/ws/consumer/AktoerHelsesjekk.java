package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.fo.veilarbaktivitet.Helsesjekk;
import no.nav.tjeneste.virksomhet.aktoer.v2.Aktoer_v2PortType;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class AktoerHelsesjekk implements Helsesjekk {

    @Inject
    private Aktoer_v2PortType aktoer_v2PortType;

    @Override
    public void helsesjekk() {
        aktoer_v2PortType.ping();
    }

}
