package no.nav.veilarbaktivitet.controller;

import io.restassured.RestAssured;
import no.nav.veilarbaktivitet.config.FilterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
public class StartTest {

    @LocalServerPort
    private int port;

    @Test
    public void kake() {
        int statusCode = RestAssured.get("http://localhost:" + port +"/veilarbaktivitet/internal").statusCode();
        assertEquals(HttpStatus.OK.value(), statusCode);
    }
}
