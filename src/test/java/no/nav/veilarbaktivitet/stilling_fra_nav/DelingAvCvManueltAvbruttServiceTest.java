package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class DelingAvCvManueltAvbruttServiceTest extends SpringBootTestBase {

    @LocalServerPort
    private int port;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    Consumer<String, DelingAvCvRespons> consumer;


    @Autowired
    DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @Autowired
    DelingAvCvCronService delingAvCvCronService;

    @AfterEach
    void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());

        consumer.unsubscribe();
        consumer.close();
    }

    @BeforeEach
    void cleanupBetweenTests() {
        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(Integer.MAX_VALUE);
    }

    @Test
    void happy_case() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        AktivitetDTO skalBehandles = aktivitetTestService.opprettStillingFraNav(mockBruker);
        AktivitetDTO skalIkkeBehandles = aktivitetTestService.opprettStillingFraNav(mockBruker);

        consumer = kafkaTestService.createStringAvroConsumer(utTopic);

        mockBruker
                .createRequest()
                .and()
                .body(skalBehandles.toBuilder().status(AktivitetStatus.AVBRUTT).avsluttetKommentar("Kake").build())
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + skalBehandles.getId() + "/status")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO originalAktivitet = aktivitetTestService.hentAktivitet(mockBruker, skalBehandles.getId());

        delingAvCvCronService.notifiserAvbruttEllerFullfortUtenSvar();

        AktivitetDTO oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBruker, skalBehandles.getId());
        AktivitetDTO expectedAktivitet = originalAktivitet.setStillingFraNavData(originalAktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_BRUKER));

        assertOppdatertAktivitet(expectedAktivitet, oppdatertAktivitet);

        final ConsumerRecord<String, DelingAvCvRespons> avbruttMelding = getSingleRecord(consumer, utTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = avbruttMelding.value();
        String bestillingsId = skalBehandles.getStillingFraNavData().bestillingsId;

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isEqualTo(skalBehandles.getId());
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.AVBRUTT);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        AktivitetDTO skalVaereUendret = aktivitetTestService.hentAktivitet(mockBruker, skalIkkeBehandles.getId());
        assertEquals(skalIkkeBehandles, skalVaereUendret);
    }


}
