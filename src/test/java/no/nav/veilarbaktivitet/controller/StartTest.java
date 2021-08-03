package no.nav.veilarbaktivitet.controller;

import io.restassured.RestAssured;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.config.FilterConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static no.nav.veilarbaktivitet.stilling_fra_nav.OpprettForesporselOmDelingAvCvTest.createMelding;
import static org.junit.Assert.assertEquals;


@EmbeddedKafka(topics = "test",  partitions = 1)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class StartTest {
    
    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> template;

    @LocalServerPort
    private int port;

    @Test
    public void kake() {
        template.send("test", "test", createMelding());
        int statusCode = RestAssured.get("http://localhost:" + port +"/veilarbaktivitet/internal").statusCode();
        assertEquals(HttpStatus.OK.value(), statusCode);
    }
}
