package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Service
public class TestService {

    public AktivitetsplanDTO gettAktiviteter(int port, String fnr) {
        Response response = given()
                .header("Content-type", "application/json")
                .get("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet?fnr=" + fnr)
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        return response.as(AktivitetsplanDTO.class);
    }

    public AktivitetDTO opprettAktivitet(int port, MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        WireMockUtil.stubBruker(mockBruker);

        String aktivitetPayloadJson = JsonUtils.toJson(aktivitetDTO);

        Response response = given()
                .header("Content-type", "application/json")
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());
        assertOpprettetAktivitet(aktivitet, aktivitetDTO);

        return aktivitet;
    }

    public void assertOpprettetAktivitet(AktivitetDTO expected, AktivitetDTO actual) {
        AktivitetDTO aktivitetDTO = expected.toBuilder()
                .id(actual.getId())
                .versjon(actual.getVersjon())
                .opprettetDato(actual.getOpprettetDato())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .lagtInnAv(actual.getLagtInnAv())
                .transaksjonsType(actual.getTransaksjonsType())
                .build();
        assertEquals(aktivitetDTO, actual);
    }
}
