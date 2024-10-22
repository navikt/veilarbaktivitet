package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static no.nav.veilarbaktivitet.util.AktivitetTestService.finnAktivitet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelingAvCvFristUtloptServiceTest extends SpringBootTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    Consumer<String, DelingAvCvRespons> consumer;

    @Autowired
    KafkaStringAvroTemplate<DelingAvCvRespons> responsKafkaTemplate;

    @Autowired
    DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @Autowired
    DelingAvCvCronService delingAvCvCronService;

    @AfterEach
    void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        Mockito.reset(responsKafkaTemplate);

        consumer.unsubscribe();
        consumer.close();
    }

    @BeforeEach
    void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
        delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(Integer.MAX_VALUE);
        consumer = kafkaTestService.createStringAvroConsumer(utTopic);
    }

    @Test
    void utlopte_aktiviteter_skal_avsluttes_automatisk() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        String uuid = UUID.randomUUID().toString();

        ForesporselOmDelingAvCv melding = AktivitetTestService.createForesporselOmDelingAvCv(uuid, mockBruker);
        melding.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));

        AktivitetDTO skalIkkeBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker);
        AktivitetDTO skalBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        delingAvCvCronService.avsluttUtlopedeAktiviteter();

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        assertEquals(skalIkkeBliAvbrutt, finnAktivitet(aktivitetsplanDTO, skalIkkeBliAvbrutt.getId()));

        AktivitetDTO skalVaereAvbrutt = finnAktivitet(aktivitetsplanDTO, skalBliAvbrutt.getId());
        AktivitetDTO expected = skalBliAvbrutt.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .stillingFraNavData(skalBliAvbrutt.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM))
                .build();

        assertOppdatertAktivitet(expected, skalVaereAvbrutt);
    }

    @Test
    void skal_ikke_oppdare_aktivitet_naar_producer_feiler() {
        MockBruker mockBruker = navMockService.createHappyBruker();

        ForesporselOmDelingAvCv melding = AktivitetTestService.createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        melding.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO skalFeile = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        ForesporselOmDelingAvCv melding2 = AktivitetTestService.createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        melding2.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO skalBliAvbrutt = aktivitetTestService.opprettStillingFraNav(mockBruker, melding2);

        Mockito
                .doThrow(IllegalStateException.class)
                .doCallRealMethod()
                .when(responsKafkaTemplate)
                .send((ProducerRecord<String, DelingAvCvRespons>) Mockito.any(ProducerRecord.class));

        //kjør første gangn 1 feiler 1 ok
        int avsluttet = delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500);
        assertEquals(2, avsluttet);

        AktivitetsplanDTO run1 = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO skalVaereAvbrutt = finnAktivitet(run1, skalBliAvbrutt.getId());
        AktivitetDTO expected = skalBliAvbrutt.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .stillingFraNavData(skalBliAvbrutt.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM))
                .build();
        assertOppdatertAktivitet(expected, skalVaereAvbrutt);

        AktivitetDTO harFeilet = finnAktivitet(run1, skalFeile.getId());
        assertEquals(skalFeile, harFeilet);

        //kjør 2. gang en aktivitet igjen går ok
        int i = delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500);
        assertEquals(1, i);

        AktivitetsplanDTO run2 = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO skaVereLikSomFeilet = finnAktivitet(run2, skalVaereAvbrutt.getId());
        assertEquals(skalVaereAvbrutt, skaVereLikSomFeilet);

        AktivitetDTO skalVaereAvbruttEtterFeil = finnAktivitet(run2, skalFeile.getId());
        AktivitetDTO expectedSkalFeile = skalFeile.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Avsluttet fordi svarfrist har utløpt")
                .stillingFraNavData(skalFeile.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.AVBRUTT_AV_SYSTEM))
                .build();
        assertOppdatertAktivitet(expectedSkalFeile, skalVaereAvbruttEtterFeil);


    }
}
