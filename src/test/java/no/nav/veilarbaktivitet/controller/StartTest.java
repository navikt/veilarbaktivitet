package no.nav.veilarbaktivitet.controller;

import io.restassured.RestAssured;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
public class StartTest {

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> template;

    @LocalServerPort
    private int port;

    @Test
    void kake() {
        int statusCode = RestAssured.get("http://localhost:" + port +"/veilarbaktivitet/internal").statusCode();
        assertEquals(HttpStatus.OK.value(), statusCode);
    }
}
