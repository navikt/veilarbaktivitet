package no.nav.veilarbaktivitet.send_paa_kafka;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.mock.AuthContextRule;
import no.nav.veilarbaktivitet.mock.TestData;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.MockBruker;
import no.nav.veilarbaktivitet.util.WireMockUtil;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static no.nav.veilarbaktivitet.mock.TestData.KJENT_SAKSBEHANDLER;
import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.*;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@EmbeddedKafka(topics = {"${topic.inn.stillingFraNav}","${topic.ut.stillingFraNav}"}, partitions = 1)
@AutoConfigureWireMock(port = 0)
@Transactional
public class JobbDAOTest {

    @LocalServerPort
    private int port;

    @Test
    public void opprettAktivitet() {

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        WireMockUtil.stubBruker(mockBruker);

        String aktivitetPayload = "{\"status\":\"PLANLAGT\",\"type\":\"MOTE\",\"tittel\":\"Blabla\",\"dato\":\"2021-09-22T11:18:21.000+02:00\",\"klokkeslett\":\"10:00\",\"varighet\":\"00:45\",\"kanal\":\"OPPMOTE\",\"adresse\":\"Video\",\"beskrivelse\":\"Vi ønsker å snakke med deg om aktiviteter du har gjennomført og videre oppfølging.\",\"forberedelser\":null,\"fraDato\":\"2021-09-22T08:00:00.000Z\",\"tilDato\":\"2021-09-22T08:45:00.000Z\"}";
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyStillingFraNav();
        Response response = given()
                .header("Content-type", "application/json")
                .and()
                .body(aktivitetPayload)
                .when()
                .post("http://localhost:"+ port + "/veilarbaktivitet/api/aktivitet/ny?fnr=1234")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO aktivitet = response.as(AktivitetDTO.class);
        assertNotNull(aktivitet);
        assertNotNull(aktivitet.getId());


    }

}