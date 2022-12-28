package no.nav.veilarbaktivitet.controller;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Transactional
public class AktivitetsplanITest {

    @LocalServerPort
    private int port;

    @Test
    public void opprettAktivitet() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        String aktivitetPayload = "{\"status\":\"PLANLAGT\",\"type\":\"MOTE\",\"tittel\":\"Blabla\",\"dato\":\"2021-09-22T11:18:21.000+02:00\",\"klokkeslett\":\"10:00\",\"varighet\":\"00:45\",\"kanal\":\"OPPMOTE\",\"adresse\":\"Video\",\"beskrivelse\":\"Vi ønsker å snakke med deg om aktiviteter du har gjennomført og videre oppfølging.\",\"forberedelser\":null,\"fraDato\":\"2021-09-22T08:00:00.000Z\",\"tilDato\":\"2021-09-22T08:45:00.000Z\"}";
        Response response = veileder
                .createRequest()
                .body(aktivitetPayload)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
    }
}