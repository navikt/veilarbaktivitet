package no.nav.veilarbaktivitet.aktivitet;

import io.restassured.response.Response;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class AktivitetsplanControllerTest extends SpringBootTestBase {

    @Test
    void veileder_skal_kunne_opprete_aktivitet() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);

        aktivitetTestService.opprettAktivitet(happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));
    }

    @Test
    void bruker_skal_kunne_opprete_aktivitet() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));
    }


    @Test
    void bruker_skal_ikke_kunne_opprete_aktivitet_på_annen_bruker() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockBruker evilUser = MockNavService.createHappyBruker();

        String aktivitetPayloadJson = JsonUtils.toJson(AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));

        Response response = evilUser
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny?fnr="+ happyBruker.getFnr())
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        AktivitetsplanDTO skalVereTomm = aktivitetTestService.hentAktiviteterForFnr(happyBruker);
        assertTrue(skalVereTomm.getAktiviteter().isEmpty());
        AktivitetsplanDTO skalHaEnAktivitet = aktivitetTestService.hentAktiviteterForFnr(evilUser);
        assertFalse(skalHaEnAktivitet.getAktiviteter().isEmpty());
    }

    @Test
    void veileder_uten_tilgang_skal_ikke_kunne_opprete_aktivitet__bruker() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder();

        String aktivitetPayloadJson = JsonUtils.toJson(AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));

        veileder
                .createRequest()
                .and()
                .body(aktivitetPayloadJson)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny?fnr="+ happyBruker.getFnr())
                .then()
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

}
