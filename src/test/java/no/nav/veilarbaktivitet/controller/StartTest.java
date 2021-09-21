package no.nav.veilarbaktivitet.controller;

import io.restassured.RestAssured;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@RunWith(SpringRunner.class)
public class StartTest {
    
    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> template;

    @LocalServerPort
    private int port;

    @Test
    public void kake() {
        int statusCode = RestAssured.get("http://localhost:" + port +"/veilarbaktivitet/internal").statusCode();
        assertEquals(HttpStatus.OK.value(), statusCode);
    }
}
