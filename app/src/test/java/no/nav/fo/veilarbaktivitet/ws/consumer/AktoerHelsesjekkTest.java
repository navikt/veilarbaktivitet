package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;

import javax.inject.Inject;

public class AktoerHelsesjekkTest extends IntegrasjonsTest{

    @Inject
    private AktoerHelsesjekk aktoerHelsesjekk;

    @Test
    public void helsesjekk_ok() {
        aktoerHelsesjekk.helsesjekk();
    }

}