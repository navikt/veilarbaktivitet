package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
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

    @Override
    public HelsesjekkMetadata getMetadata() {
        String aktoerUrl = System.getProperty("aktoer.endpoint.url");
        return new HelsesjekkMetadata(
                "virksomhet:Aktoer_v2 via " + aktoerUrl,
                "Ping av aktoer_v2 (hente aktørid).",
                true
        );
    }

}
