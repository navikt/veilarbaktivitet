package no.nav.veilarbaktivitet.internapi;

import io.restassured.response.Response;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Egenaktivitet;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class InternApiControllerTest extends SpringBootTestBase {

    @Test
    public void sushi() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO moteAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitetSomVeileder(port, mockVeileder, mockBruker, moteAktivitet);

        // Test "/internal/api/v1/aktivitet/{aktivitetId}"
        Mote aktivitet = mockVeileder.createRequest()
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

        AktivitetData aktivitetData2 = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO egenAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData2, false);

        aktivitetTestService.opprettAktivitet(port, mockBruker, egenAktivitet);

        // Test "/internal/api/v1/aktivitet/"
        Response response = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();
        List<Aktivitet> aktiviteter = response.jsonPath().getList(".", Aktivitet.class);

        assertThat(aktiviteter.size()).isEqualTo(2);
        assertThat(aktiviteter.get(1)).isInstanceOf(Egenaktivitet.class);
    }
}