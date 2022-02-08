package no.nav.veilarbaktivitet.internapi;

import io.restassured.response.Response;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.internapi.model.Mote;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Date;


public class InternApiControllerTest extends SpringBootTestBase {

    @Test
    public void sushi() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO moteAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        // Opprett møteaktivitet
        Response response = veileder
                .createRequest()
                .body(moteAktivitet)
                .when()
                .post("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/ny?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO opprettetAktivitet = response.as(AktivitetDTO.class);

        Mote aktivitet = veileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet/{aktivitetId}", opprettetAktivitet.getId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(Mote.class);

        Assertions.assertThat(aktivitet).isInstanceOf(Mote.class);

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(aktivitet.getAktivitetType().name()).isEqualTo(opprettetAktivitet.getType().name());
            a.assertThat(Date.from(aktivitet.getOpprettetDato().toInstant())).isEqualTo(opprettetAktivitet.getOpprettetDato());
            a.assertThat(aktivitet.getReferat()).isEqualTo(opprettetAktivitet.getReferat());
            a.assertAll();
        });
    }

}