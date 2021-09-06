package no.nav.veilarbaktivitet.util;

import io.restassured.response.Response;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Service
public class ITestService {

    @Autowired
    ConsumerFactory consumerFactory;


    /**
     * Lager en ny kafka consumer med random groupid på topic som leser fra slutten av topic.
     * Meldinger kan leses ved å bruke utility metoder i  KafkaTestUtils
     * @see org.springframework.kafka.test.utils.KafkaTestUtils#getSingleRecord(org.apache.kafka.clients.consumer.Consumer, java.lang.String, long)
     * @param topic Topic du skal lese fra
     * @return En kafka consumer
     */
    public Consumer createConsumer(String topic) {
        String randomGroup = UUID.randomUUID().toString();
        Properties modifisertConfig = new Properties();
        modifisertConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Consumer newConsumer = consumerFactory.createConsumer(randomGroup, null, null, modifisertConfig);

        List<PartitionInfo> partitionInfos = newConsumer.partitionsFor(topic);
        List<TopicPartition> collect = partitionInfos.stream().map(f -> new TopicPartition(topic, f.partition())).collect(Collectors.toList());

        newConsumer.assign(collect);
        newConsumer.seekToEnd(collect);

        collect.forEach(a -> newConsumer.position(a, Duration.ofSeconds(10)));

        newConsumer.commitSync(Duration.ofSeconds(10));
        return newConsumer;
    }

    /**
     * Henter alle aktiviteter for et fnr via aktivitet-apiet.
     * @param port Portnummeret til webserveren.
     *             Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{@LocalServerPort private int port;}
     * @param fnr
     * @return En AktivitetplanDTO med en liste av AktivitetDto
     */
    public AktivitetsplanDTO hentAktiviteterForFnr(int port, String fnr) {
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


    /**
     * Oppretter en ny aktivitet via aktivitet-apiet. Kallet blir utført av nav-bruker no.nav.veilarbaktivitet.config.FilterTestConfig#NAV_IDENT_ITEST Z123456
     * @param port Portnummeret til webserveren.
     *             Når man bruker SpringBootTest.WebEnvironment.RANDOM_PORT, kan portnummeret injektes i testklassen ved å bruke @code{@LocalServerPort private int port;}
     * @param mockBruker Brukeren man skal opprette aktiviteten for
     * @param aktivitetDTO payload
     * @return Aktiviteten
     */
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
                // overskriv system-genererte attributter
                .id(actual.getId())
                .versjon(actual.getVersjon())
                .opprettetDato(actual.getOpprettetDato())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .lagtInnAv(actual.getLagtInnAv())
                .transaksjonsType(actual.getTransaksjonsType())
                .build();
        // sammenlign resten - forutsetter implementert equals
        assertEquals(aktivitetDTO, actual);
    }

    public void assertOppdatertAktivitet(AktivitetDTO expected, AktivitetDTO actual) {
        AktivitetDTO aktivitetDTO = expected.toBuilder()
                // overskriv system-genererte attributter
                .versjon(actual.getVersjon())
                .endretDato(actual.getEndretDato())
                .endretAv(actual.getEndretAv())
                .lagtInnAv(actual.getLagtInnAv())
                .transaksjonsType(actual.getTransaksjonsType())
                .build();
        // sammenlign resten - forutsetter implementert equals
        assertEquals(aktivitetDTO, actual);
    }
}
