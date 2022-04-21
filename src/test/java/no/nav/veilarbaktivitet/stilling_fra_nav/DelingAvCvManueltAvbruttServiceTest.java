package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class DelingAvCvManueltAvbruttServiceTest {

    @Autowired
    KafkaTestService testService;

    AktivitetTestService aktivitetTestService;
    @Autowired
    StillingFraNavTestService stillingFraNavTestService;

    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> producer;

    Consumer<String, DelingAvCvRespons> consumer;


    @Autowired
    DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @Autowired
    DelingAvCvCronService delingAvCvCronService;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());

        consumer.unsubscribe();
        consumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        aktivitetTestService = new AktivitetTestService(stillingFraNavTestService, port);
        DbTestUtils.cleanupTestDb(jdbc);
        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(Integer.MAX_VALUE);
    }

    @Test
    public void happy_case() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetDTO skalBehandles = aktivitetTestService.opprettStillingFraNav(mockBruker);
        AktivitetDTO skalIkkeBehandles = aktivitetTestService.opprettStillingFraNav(mockBruker);

        consumer = testService.createStringAvroConsumer(utTopic);

        mockBruker
                .createRequest()
                .and()
                .body(skalBehandles.toBuilder().status(AktivitetStatus.AVBRUTT).avsluttetKommentar("Kake").build())
                .when()
                .put(mockBruker.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + skalBehandles.getId() + "/status", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO originalAktivitet = aktivitetTestService.hentAktivitet(mockBruker, skalBehandles.getId());

        delingAvCvCronService.notifiserAvbruttEllerFullfortUtenSvar();

        AktivitetDTO oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBruker, skalBehandles.getId());
        AktivitetDTO expectedAktivitet = originalAktivitet.setStillingFraNavData(originalAktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_BRUKER));

        assertOppdatertAktivitet(expectedAktivitet, oppdatertAktivitet);

        final ConsumerRecord<String, DelingAvCvRespons> avbruttMelding = getSingleRecord(consumer, utTopic, 10000);
        DelingAvCvRespons value = avbruttMelding.value();
        String bestillingsId = skalBehandles.getStillingFraNavData().bestillingsId;

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isEqualTo(skalBehandles.getId());
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.AVBRUTT);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        AktivitetDTO skalVaereUendret = aktivitetTestService.hentAktivitet(mockBruker, skalIkkeBehandles.getId());
        assertEquals(skalIkkeBehandles, skalVaereUendret);
    }


}