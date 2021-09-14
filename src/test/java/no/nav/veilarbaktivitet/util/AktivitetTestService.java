package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.RestassureddUser;
import no.nav.veilarbaktivitet.testutils.AktivietAssertUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static org.junit.Assert.assertNotNull;

@Service
public class AktivitetTestService {

    /**
     * Henter alle aktiviteter for et fnr via aktivitet-apiet.
     *
     * @param port       Portnummeret til webserveren.
     *                   Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{@LocalServerPort private int port;}
     * @param mockBruker
     * @return En AktivitetplanDTO med en liste av AktivitetDto
     */
    public AktivitetsplanDTO hentAktiviteterForFnr(int port, MockBruker mockBruker) {
        return hentAktiviteterForFnr(port, mockBruker, mockBruker);
    }


    public AktivitetsplanDTO hentAktiviteterForFnr(int port, MockBruker mockBruker, RestassureddUser user) {
        Response response = user
                .createRequest()
                .get(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet", mockBruker))
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        return response.as(AktivitetsplanDTO.class);
    }


    /**
     * Oppretter en ny aktivitet via aktivitet-apiet. Kallet blir utført av nav-bruker no.nav.veilarbaktivitet.config.FilterTestConfig#NAV_IDENT_ITEST Z123456
     *
     * @param port         Portnummeret til webserveren.
     *                     Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{@LocalServerPort private int port;}
     * @param mockBruker   Brukeren man skal opprette aktiviteten for
     * @param aktivitetDTO payload
     * @return Aktiviteten
     */
    public AktivitetDTO opprettAktivitet(int port, MockBruker mockBruker, AktivitetDTO aktivitetDTO) {

        return opprettAktivitet(port, mockBruker, mockBruker, aktivitetDTO);
    }

    public AktivitetDTO opprettAktivitet(int port, MockBruker mockBruker, RestassureddUser user, AktivitetDTO aktivitetDTO) {

        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = user
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post(user.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        AktivietAssertUtils.assertOpprettetAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }
}
