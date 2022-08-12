package no.nav.veilarbaktivitet.stilling_fra_nav;

import ch.qos.logback.classic.Level;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.*;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
import no.nav.veilarbaktivitet.util.MemoryLoggerAppender;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static no.nav.veilarbaktivitet.util.AktivitetTestService.createForesporselOmDelingAvCv;
import static no.nav.veilarbaktivitet.veilarbportefolje.CvKanDelesStatus.JA;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Slf4j
public class DelingAvCvITest extends SpringBootTestBase {

    @Value("${topic.inn.stillingFraNav}")
    private String stillingFraNavForespurt;

    @Value("${topic.ut.stillingFraNav}")
    private String stillingFraNavOppdatert;


    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> foresporselOmDelingAvCvProducer;


    Consumer<String, DelingAvCvRespons> delingAvCvResponsConsumer;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        delingAvCvResponsConsumer.unsubscribe();
        delingAvCvResponsConsumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        delingAvCvResponsConsumer = kafkaTestService.createStringAvroConsumer(stillingFraNavOppdatert);

    }

    @Test
    public void happy_case() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        var brukernotifikajonOppgave = brukernotifikasjonAsserts.oppgaveSendt(mockBruker.getFnrAsFnr(), aktivitetDTO);
        var oppgave = brukernotifikajonOppgave.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Kan denne stillingen passe for deg? Vi leter etter jobbsøkere for en arbeidsgiver.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(brukernotifikajonOppgave.key().getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }


    @Test
    public void happy_case_tomme_strenger() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        KontaktInfo kontaktinfo = KontaktInfo.newBuilder().setMobil("").setNavn("").setTittel("").build();
        melding.setKontaktInfo(kontaktinfo);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        var brukernotifikajonOppgave = brukernotifikasjonAsserts.oppgaveSendt(mockBruker.getFnrAsFnr(), aktivitetDTO);
        var oppgave = brukernotifikajonOppgave.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Kan denne stillingen passe for deg? Vi leter etter jobbsøkere for en arbeidsgiver.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(brukernotifikajonOppgave.key().getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    public void happy_case_ingen_kontaktInfo_ingen_soknadsfrist() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        melding.setKontaktInfo(null);
        melding.setSoknadsfrist(null);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, melding);

        var brukernotifikajonOppgave = brukernotifikasjonAsserts.oppgaveSendt(mockBruker.getFnrAsFnr(), aktivitetDTO);
        var oppgave = brukernotifikajonOppgave.value();

        SoftAssertions.assertSoftly(assertions -> { //todo tenkpå cpy paste fra testen over
            assertions.assertThat(oppgave.getTekst()).isEqualTo("Kan denne stillingen passe for deg? Vi leter etter jobbsøkere for en arbeidsgiver.");
            assertions.assertThat(oppgave.getEksternVarsling()).isEqualTo(true);
            assertions.assertThat(brukernotifikajonOppgave.key().getFodselsnummer()).isEqualTo(mockBruker.getFnr());
            assertions.assertThat(oppgave.getLink()).isEqualTo(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetDTO.getId());
            assertions.assertAll();
        });
    }

    @Test
    public void ugyldig_aktorid() {
        MemoryLoggerAppender memoryLoggerAppender = MemoryLoggerAppender.getMemoryAppenderForLogger("no.nav.veilarbaktivitet");

        //TODO se på om vi burde unngå bruker her
        MockBruker mockBruker = MockNavService.createHappyBruker();

        WireMockUtil.aktorUtenGjeldende(mockBruker.getFnr(), mockBruker.getAktorId());


        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        ListenableFuture<SendResult<String, ForesporselOmDelingAvCv>> send = foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_OPPRETTE);
            assertions.assertThat(value.getKanIkkeOppretteBegrunnelse().getFeilmelding()).isEqualTo("Finner ingen gyldig ident for aktorId");
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
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
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
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
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
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
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
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);


        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
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
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
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
        MockBruker mockBruker = MockNavService.createHappyBruker();

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(delingAvCvResponsConsumer, stillingFraNavOppdatert, 10000);
        DelingAvCvRespons value = record.value();
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        ForesporselOmDelingAvCv duplikatMelding = createForesporselOmDelingAvCv(bestillingsId, mockBruker);
        SendResult<String, ForesporselOmDelingAvCv> result = foresporselOmDelingAvCvProducer.send(stillingFraNavForespurt, duplikatMelding.getBestillingsId(), duplikatMelding).get();
        kafkaTestService.assertErKonsumertAiven(stillingFraNavForespurt, result.getRecordMetadata().offset(), 5);

        ConsumerRecords<String, DelingAvCvRespons> poll = delingAvCvResponsConsumer.poll(Duration.ofMillis(100));
        assertTrue(poll.isEmpty());
    }

}
