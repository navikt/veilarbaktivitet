package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import no.nav.common.auth.context.UserRole;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.config.TestAuthContextFilter;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.testutils.AktivietAssertUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;

@Service
public class AktivitetTestService {

    /**
     * Henter alle aktiviteter for et fnr via aktivitet-apiet.
     *
     * @param port Portnummeret til webserveren.
     *             Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{@LocalServerPort private int port;}
     * @param fnr
     * @return En AktivitetplanDTO med en liste av AktivitetDto
     */
    public AktivitetsplanDTO hentAktiviteterForFnr(int port, String fnr) {
        Response response = given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilter.identHeder, fnr)
                .header(TestAuthContextFilter.typeHeder, UserRole.EKSTERN)
                .get("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet")
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

        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilter.identHeder, mockBruker.getFnr())
                .header(TestAuthContextFilter.typeHeder, UserRole.EKSTERN)
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny")
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
