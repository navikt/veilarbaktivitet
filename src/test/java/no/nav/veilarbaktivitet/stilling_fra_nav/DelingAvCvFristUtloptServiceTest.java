package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
    KafkaTemplate<String, DelingAvCvRespons> responsKafkaTemplate;

    @Autowired
    DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        Mockito.reset(responsKafkaTemplate);

        consumer.unsubscribe();
        consumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(Integer.MAX_VALUE);
        consumer = testService.createStringAvroConsumer(utTopic);
    }

    @Test
    public void utlopte_aktiviteter_skal_avsluttes_automatisk() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        String uuid = UUID.randomUUID().toString();

        ForesporselOmDelingAvCv melding = AktivitetTestService.createMelding(uuid, mockBruker);
        melding.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));

        AktivitetDTO skalIkkeBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        AktivitetDTO skalBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, melding, port);

        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter();

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker);
        assertEquals(skalIkkeBliAvbrutt, finnAktivitet(aktivitetsplanDTO, skalIkkeBliAvbrutt.getId()));

        AktivitetDTO skalVaereAvbrutt = finnAktivitet(aktivitetsplanDTO, skalBliAvbrutt.getId());
        AktivitetDTO expected = skalBliAvbrutt.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .build();

        assertOppdatertAktivitet(expected, skalVaereAvbrutt);
    }

    @Test
    public void skal_ikke_oppdare_aktivitet_naar_producer_feiler() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        ForesporselOmDelingAvCv melding = AktivitetTestService.createMelding(UUID.randomUUID().toString(), mockBruker);
        melding.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO skalFeile = aktivitetTestService.opprettStillingFraNav(mockBruker, melding, port);

        ForesporselOmDelingAvCv melding2 = AktivitetTestService.createMelding(UUID.randomUUID().toString(), mockBruker);
        melding2.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO skalBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, melding2, port);

        Mockito
                .doThrow(IllegalStateException.class)
                .doCallRealMethod()
                .when(responsKafkaTemplate)
                .send((ProducerRecord<String, DelingAvCvRespons>) Mockito.any(ProducerRecord.class));

        //kjør første gangn 1 feiler 1 ok
        int avsluttet = delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500);
        assertEquals(2, avsluttet);

        AktivitetsplanDTO run1 = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker);
        AktivitetDTO skalVaereAvbrutt = finnAktivitet(run1, skalBliAvbrutt.getId());
        AktivitetDTO expected = skalBliAvbrutt.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .build();
        assertOppdatertAktivitet(expected, skalVaereAvbrutt);

        AktivitetDTO harFeilet = finnAktivitet(run1, skalFeile.getId());
        assertEquals(skalFeile, harFeilet);

        //kjør 2. gang en aktivitet igjen går ok
        int i = delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500);
        assertEquals(1, i);

        AktivitetsplanDTO run2 = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker);
        AktivitetDTO skaVereLikSomFeilet = finnAktivitet(run2, skalVaereAvbrutt.getId());
        assertEquals(skalVaereAvbrutt, skaVereLikSomFeilet);

        AktivitetDTO skalVaereAvbruttEtterFeil = finnAktivitet(run2, skalFeile.getId());
        AktivitetDTO expectedSkalFeile = skalFeile.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .build();
        assertOppdatertAktivitet(expectedSkalFeile, skalVaereAvbruttEtterFeil);


    }
}