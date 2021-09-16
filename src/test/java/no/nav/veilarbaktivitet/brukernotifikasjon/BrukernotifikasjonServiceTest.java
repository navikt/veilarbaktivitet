package no.nav.veilarbaktivitet.brukernotifikasjon;

import no.nav.veilarbaktivitet.util.AktivitetTestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class BrukernotifikasjonServiceTest {

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Test
    public void oppgave_er_rettet() {
        assertTrue(true);
    }
}