package no.nav.veilarbaktivitet.stilling_fra_nav;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
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

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;
    /***** Ekte bønner *****/

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    Consumer<String, DelingAvCvRespons> consumer;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());

        consumer.unsubscribe();
        consumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);

        consumer = testService.createConsumer(utTopic);
    }

    @Test
    public void happy_case() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker.getFnr());

        assertEquals(1, aktivitetsplanDTO.aktiviteter.size());
        AktivitetDTO aktivitetDTO = aktivitetsplanDTO.getAktiviteter().get(0);

        //TODO skriv bedre test
        assertEquals(AktivitetTypeDTO.STILLING_FRA_NAV, aktivitetDTO.getType());
        assertEquals(melding.getStillingstittel(), aktivitetDTO.getTittel());
        assertEquals("/rekrutteringsbistand/" + melding.getStillingsId(), aktivitetDTO.getLenke());
        assertEquals(melding.getBestillingsId(), aktivitetDTO.getStillingFraNavData().bestillingsId);

    }

    @Test
    public void ugyldig_aktorid() {
        MemoryLoggerAppender memoryLoggerAppender = MemoryLoggerAppender.getMemoryAppenderForLogger("no.nav.veilarbaktivitet");

        //TODO se på om vi burde ungå bruker her
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
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        ListenableFuture<SendResult<String, ForesporselOmDelingAvCv>> send = producer.send(innTopic, melding.getBestillingsId(), melding);
        await().atMost(5, SECONDS).until(() -> testService.erKonsumert(innTopic, groupId, send.get().getRecordMetadata().offset()));
        assertTrue(memoryLoggerAppender.contains("*** Kan ikke behandle melding", Level.ERROR));
    }

    @Test
    public void ikke_under_oppfolging() {

        BrukerOptions options = BrukerOptions.happyBrukerBuilder().underOppfolging(false).build();
        MockBruker mockBruker = MockNavService.creteBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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
        MockBruker mockBruker = MockNavService.creteBruker(brukerOptions);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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
        MockBruker mockBruker = MockNavService.creteBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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
        MockBruker mockBruker = MockNavService.creteBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());


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
        MockBruker mockBruker = MockNavService.creteBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
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

        ForesporselOmDelingAvCv duplikatMelding = createMelding(bestillingsId, mockBruker.getAktorId());
        SendResult<String, ForesporselOmDelingAvCv> result = producer.send(innTopic, duplikatMelding.getBestillingsId(), duplikatMelding).get();
        await().atMost(5, SECONDS).until(() -> testService.erKonsumert(innTopic, groupId, result.getRecordMetadata().offset()));

        ConsumerRecords<String, DelingAvCvRespons> poll = consumer.poll(Duration.ofMillis(100));
        assertTrue(poll.isEmpty());
    }

    static ForesporselOmDelingAvCv createMelding(String bestillingsId, String aktorId) {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(aktorId)
                .setArbeidsgiver("arbeidsgiver")
                .setArbeidssteder(List.of(
                        Arbeidssted.newBuilder()
                                .setAdresse("adresse")
                                .setPostkode("1234")
                                .setKommune("kommune")
                                .setBy("by")
                                .setFylke("fylke")
                                .setLand("land").build(),
                        Arbeidssted.newBuilder()
                                .setAdresse("VillaRosa")
                                .setPostkode(null)
                                .setKommune(null)
                                .setBy(null)
                                .setFylke(null)
                                .setLand("spania").build()))
                .setBestillingsId(bestillingsId)
                .setOpprettet(Instant.now())
                .setOpprettetAv("Z999999")
                .setCallId("callId")
                .setSoknadsfrist("10102021")
                .setStillingsId("stillingsId")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .setKontaktInfo(KontaktInfo.newBuilder()
                        .setNavn("Jan Saksbehandler")
                        .setTittel("Nav-ansatt")
                        .setEpost("jan.saksbehandler@nav.no")
                        .setMobil("99999999").build()
                )
                .build();
    }

}
