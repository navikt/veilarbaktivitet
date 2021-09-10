package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import no.nav.veilarbaktivitet.util.MockBruker;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static no.nav.veilarbaktivitet.util.AktivitetTestService.finnAktivitet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class DelingAvCvFristUtloptServiceTest {

    @Autowired
    KafkaTestService testService;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Value("${topic.inn.stillingFraNav}")
    private String innTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    Consumer<String, DelingAvCvRespons> consumer;

    @Autowired
    DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());

        consumer.unsubscribe();
        consumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(Integer.MAX_VALUE);
        consumer = testService.createConsumer(utTopic);
    }

    @Test
    public void utlopte_aktiviteter_skal_avsluttes_automatisk() {
        MockBruker mockBruker = MockBruker.happyBruker();
        String uuid = UUID.randomUUID().toString();

        ForesporselOmDelingAvCv melding = AktivitetTestService.createMelding(uuid, mockBruker);
        melding.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));

        AktivitetDTO skalIkkeBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        AktivitetDTO skalBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, melding, port);

        int avsluttet = delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500);
        assertEquals(1, avsluttet);

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker.getFnr());
        assertEquals(skalIkkeBliAvbrutt, finnAktivitet(aktivitetsplanDTO, skalIkkeBliAvbrutt.getId()));

        AktivitetDTO skalVaereAvbrutt = finnAktivitet(aktivitetsplanDTO, skalBliAvbrutt.getId());
        AktivitetDTO expected = skalBliAvbrutt.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .build();

        assertOppdatertAktivitet(expected, skalVaereAvbrutt);
    }
}