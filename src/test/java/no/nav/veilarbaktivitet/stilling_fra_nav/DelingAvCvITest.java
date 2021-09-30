package no.nav.veilarbaktivitet.stilling_fra_nav;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import no.nav.veilarbaktivitet.util.MemoryLoggerAppender;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.veilarbaktivitet.util.AktivitetTestService.createMelding;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class DelingAvCvITest {

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

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    Consumer<String, DelingAvCvRespons> consumer;

    Consumer<Nokkel, Oppgave> oppgaveConsumer;

    @Autowired
    SendOppgaveCron sendOppgaveCron;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());

        consumer.unsubscribe();
        consumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);

        consumer = testService.createStringAvroConsumer(utTopic);
        oppgaveConsumer = testService.createAvroAvroConsumer(oppgaveTopic);
    }

    @Test
    public void happy_case() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);

        sendOppgaveCron.sendBrukernotifikasjoner();
        final ConsumerRecord<Nokkel, Oppgave> consumerRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        Oppgave oppgave = consumerRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Her en stilling som NAV tror kan passe for deg. Gi oss en tilbakemelding.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(oppgave.getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }


    @Test
    public void happy_case_tomme_strenger() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        ForesporselOmDelingAvCv melding = createMelding(UUID.randomUUID().toString(), mockBruker);
        KontaktInfo kontaktinfo = KontaktInfo.newBuilder().setMobil("").setNavn("").setTittel("").build();
        melding.setKontaktInfo(kontaktinfo);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding, port);

        sendOppgaveCron.sendBrukernotifikasjoner();
        final ConsumerRecord<Nokkel, Oppgave> consumerRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        Oppgave oppgave = consumerRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Her en stilling som NAV tror kan passe for deg. Gi oss en tilbakemelding.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(oppgave.getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    public void happy_case_ingen_kontaktInfo() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        ForesporselOmDelingAvCv melding = createMelding(UUID.randomUUID().toString(), mockBruker);
        melding.setKontaktInfo(null);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding, port);

        sendOppgaveCron.sendBrukernotifikasjoner();
        final ConsumerRecord<Nokkel, Oppgave> consumerRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        Oppgave oppgave = consumerRecord.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Her en stilling som NAV tror kan passe for deg. Gi oss en tilbakemelding.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(oppgave.getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    public void ugyldig_aktorid() {
        MemoryLoggerAppender memoryLoggerAppender = MemoryLoggerAppender.getMemoryAppenderForLogger("no.nav.veilarbaktivitet");

        //TODO se på om vi burde unngå bruker her
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        stubFor(get("/aktorTjeneste/identer?gjeldende=true&identgruppe=NorskIdent")
                .withHeader("Nav-Personidenter", equalTo(mockBruker.getAktorId()))
                .willReturn(ok().withBody("" +
                        "{" +
                        "  \"" + mockBruker.getAktorId() + "\": {" +
                        "    \"identer\": []" +
                        "  }" +
                        "}")));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        ListenableFuture<SendResult<String, ForesporselOmDelingAvCv>> send = producer.send(innTopic, melding.getBestillingsId(), melding);
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getKanIkkeOppretteBegrunnelse().getFeilmelding()).isEqualTo("Finner ingen gydlig ident for aktorId");
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
        assertTrue(memoryLoggerAppender.contains("*** Kan ikke behandle melding", Level.WARN));
    }

    @Test
    public void ikke_under_oppfolging() {

        BrukerOptions options = BrukerOptions.happyBrukerBuilder().underOppfolging(false).build();
        MockBruker mockBruker = MockNavService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    public void under_oppfolging_kvp() {
        BrukerOptions brukerOptions = BrukerOptions.happyBrukerBuilder().erUnderKvp(true).underOppfolging(true).build();
        MockBruker mockBruker = MockNavService.createBruker(brukerOptions);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void under_manuell_oppfolging() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().erManuell(true).build();
        MockBruker mockBruker = MockNavService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void reservert_i_krr() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().erReservertKrr(true).build();
        MockBruker mockBruker = MockNavService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);


        producer.send(innTopic, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void mangler_nivaa4() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().harBruktNivaa4(false).build();
        MockBruker mockBruker = MockNavService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        producer.send(innTopic, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    @SneakyThrows
    public void duplikat_bestillingsId_ignoreres() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        ForesporselOmDelingAvCv duplikatMelding = createMelding(bestillingsId, mockBruker);
        SendResult<String, ForesporselOmDelingAvCv> result = producer.send(innTopic, duplikatMelding.getBestillingsId(), duplikatMelding).get();
        await().atMost(5, SECONDS).until(() -> testService.erKonsumert(innTopic, groupId, result.getRecordMetadata().offset()));

        ConsumerRecords<String, DelingAvCvRespons> poll = consumer.poll(Duration.ofMillis(100));
        assertTrue(poll.isEmpty());
    }
}
