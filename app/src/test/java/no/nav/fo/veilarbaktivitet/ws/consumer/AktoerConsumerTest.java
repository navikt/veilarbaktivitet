package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.TestData;
import org.junit.Test;

import javax.inject.Inject;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.junit.Assert.assertThat;

public class AktoerConsumerTest extends IntegrasjonsTest {

    private static final String IKKE_EKSISTERENDE_IDENT = "12345678";

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Test
    public void hentAktoerIdForIdent_ikkeEksisterendeIdent_ikkeFunnet() {
        assertThat(aktoerConsumer.hentAktoerIdForIdent(IKKE_EKSISTERENDE_IDENT), isEmpty());
    }

    @Test
    public void hentAktoerIdForIdent_kjentTestIdent_kjentTestAktor() {
        assertThat(aktoerConsumer.hentAktoerIdForIdent(TestData.KJENT_IDENT), hasValue(TestData.KJENT_AKTOR_ID));
    }

}
