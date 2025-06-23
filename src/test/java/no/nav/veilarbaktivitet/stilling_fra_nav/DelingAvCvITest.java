package no.nav.veilarbaktivitet.stilling_fra_nav;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.util.MemoryLoggerAppender;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.UUID;

import static no.nav.veilarbaktivitet.util.AktivitetTestService.createForesporselOmDelingAvCv;
import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Slf4j
class DelingAvCvITest extends SpringBootTestBase {

    @Value("${topic.inn.stillingFraNav}")
    private String stillingFraNavForespurtTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String stillingFraNavOppdatertTopic;

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> foresporselOmDelingAvCvProducer;


    Consumer<String, DelingAvCvRespons> delingAvCvResponsConsumer;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @AfterEach
    void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        delingAvCvResponsConsumer.unsubscribe();
        delingAvCvResponsConsumer.close();
    }

    @BeforeEach
    void cleanupBetweenTests() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        delingAvCvResponsConsumer = kafkaTestService.createStringAvroConsumer(stillingFraNavOppdatertTopic);

    }

    @Test
    void når_motatt_stilling_fra_nav_skal_det_sendes_ut_en_brukernotifikasjons_oppgave() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekster().getFirst().getTekst()).isEqualTo("Vi søker etter kandidater til denne stillingen. Kan denne stillingen passe for deg?");
            assertions.assertThat(oppgave.getEksternVarsling()).isNotNull();
            assertions.assertThat(oppgave.getIdent()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }


    @Test
    void happy_case_tomme_strenger() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        KontaktInfo kontaktinfo = KontaktInfo.newBuilder().setMobil("").setNavn("").setTittel("").build();
        melding.setKontaktInfo(kontaktinfo);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekster().getFirst().getTekst()).isEqualTo("Vi søker etter kandidater til denne stillingen. Kan denne stillingen passe for deg?");
            assertions.assertThat(oppgave.getEksternVarsling()).isNotNull();
            assertions.assertThat(oppgave.getIdent()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    void happy_case_ingen_kontaktInfo_ingen_soknadsfrist() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        melding.setKontaktInfo(null);
        melding.setSoknadsfrist(null);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        SoftAssertions.assertSoftly(assertions -> { //todo tenkpå cpy paste fra testen over
            assertions.assertThat(oppgave.getTekster().getFirst().getTekst()).isEqualTo("Vi søker etter kandidater til denne stillingen. Kan denne stillingen passe for deg?");
            assertions.assertThat(oppgave.getEksternVarsling()).isNotNull();
            assertions.assertThat(oppgave.getIdent()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    void ugyldig_aktorid() {
        MemoryLoggerAppender memoryLoggerAppender = MemoryLoggerAppender.getMemoryAppenderForLogger("SecureLog");

        //TODO se på om vi burde unngå bruker her
        MockBruker mockBruker = navMockService.createBruker(BrukerOptions.happyBrukerBuilder().underOppfolging(false).build());

        WireMockUtil.aktorUtenGjeldende(mockBruker.getFnr(), mockBruker.getAktorId());


        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getKanIkkeOppretteBegrunnelse().getFeilmelding()).isEqualTo("Finner ingen gyldig ident for aktorId");
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
        assertTrue(memoryLoggerAppender.contains("*** Kan ikke behandle melding", Level.WARN));
    }

    @Test
    void ikke_under_oppfolging() {

        BrukerOptions options = BrukerOptions.happyBrukerBuilder().underOppfolging(false).build();
        MockBruker mockBruker = navMockService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    void under_oppfolging_kvp() {
        BrukerOptions brukerOptions = BrukerOptions.happyBrukerBuilder().erUnderKvp(true).underOppfolging(true).build();
        MockBruker mockBruker = navMockService.createBruker(brukerOptions);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    void under_manuell_oppfolging() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().erManuell(true).build();
        MockBruker mockBruker = navMockService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    void reservert_i_krr() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().erReservertKrr(true).build();
        MockBruker mockBruker = navMockService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);


        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    void bruker_som_ikke_kan_varsles_skal_fa_tilstand_kan_ikke_varsle() {
        BrukerOptions options = BrukerOptions.happyBrukerBuilder().erManuell(true).build();
        MockBruker mockBruker = navMockService.createBruker(options);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    @SneakyThrows
    void duplikat_bestillingsId_ignoreres() {
        MockBruker mockBruker = navMockService.createHappyBruker();

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatertTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        DelingAvCvRespons value = record.value();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        ForesporselOmDelingAvCv duplikatMelding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        SendResult<String, ForesporselOmDelingAvCv> result = foresporselOmDelingAvCvProducer.send(stillingFraNavForespurtTopic, duplikatMelding.getBestillingsId(), duplikatMelding).get();
        kafkaTestService.assertErKonsumert(stillingFraNavForespurtTopic, result.getRecordMetadata().offset());

        ConsumerRecords<String, DelingAvCvRespons> poll = delingAvCvResponsConsumer.poll(Duration.ofMillis(100));
        assertTrue(poll.isEmpty());
    }

}
