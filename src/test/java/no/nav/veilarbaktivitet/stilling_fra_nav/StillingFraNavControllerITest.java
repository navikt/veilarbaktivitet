package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.*;
import no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class StillingFraNavControllerITest {

    public static final Date AVTALT_DATO = new Date(2021, Calendar.MAY, 4);
    @Autowired
    KafkaTestService testService;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;
    @Autowired
    SendOppgaveCron sendOppgaveCron;
    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;
    @LocalServerPort
    private int port;
    @Value("${topic.inn.stillingFraNav}")
    private String innTopic;
    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;
    @Value("${topic.ut.brukernotifikasjon.done}")
    private String brukernotifkasjonFerdigToppik;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
    }

    @Test
    public void happy_case_svar_ja() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        sendOppgaveCron.sendBrukernotifikasjoner();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createStringAvroConsumer(utTopic);

        AktivitetDTO svartJaPaaDelingAvCv = svarJaPaaDelingAvCv(mockBruker, veileder, aktivitetDTO);

        assertAktivitetSvartJa(veileder, aktivitetDTO, svartJaPaaDelingAvCv);
        assertSentSvarTilRekruteringsbistand(mockBruker, veileder, aktivitetDTO, consumer, true);
        assertBrukernotifikasjonStoppet(mockBruker);

        skalKunneOppdatereSoknadStatus(mockBruker, veileder, svartJaPaaDelingAvCv);
    }

    @Test
    public void happy_case_svar_nei() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .avtaltDato(AVTALT_DATO)
                .kanDeles(false)
                .build();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createStringAvroConsumer(utTopic);

        Response response = veileder
                .createRequest()
                .and()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO actualAktivitet = response.as(AktivitetDTO.class);

        CvKanDelesData expectedCvKanDelesData = CvKanDelesData.builder()
                .kanDeles(false)
                .endretAv(veileder.getNavIdent())
                .endretAvType(InnsenderData.NAV)
                .avtaltDato(AVTALT_DATO)
                // kopierer systemgenererte attributter
                .endretTidspunkt(actualAktivitet.getStillingFraNavData().getCvKanDelesData().endretTidspunkt)
                .build();

        StillingFraNavData stillingFraNavData = aktivitetDTO.getStillingFraNavData().toBuilder()
                .cvKanDelesData(expectedCvKanDelesData)
                .livslopsStatus(LivslopsStatus.HAR_SVART)
                .build();

        AktivitetDTO expectedAktivitet = aktivitetDTO.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .stillingFraNavData(stillingFraNavData)
                .avsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles")
                .build();

        assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);

        // Sjekk at svarmelding sendt til rekrutteringsbistand
        assertSentSvarTilRekruteringsbistand(mockBruker, veileder, aktivitetDTO, consumer, false);
    }

    @Test
    public void svar_naar_frist_utlopt_feiler() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        ForesporselOmDelingAvCv foresporselFristUtlopt = AktivitetTestService.createMelding(UUID.randomUUID().toString(), mockBruker);
        foresporselFristUtlopt.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, foresporselFristUtlopt, port);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .avtaltDato(AVTALT_DATO)
                .kanDeles(false)
                .build();

        veileder
                .createRequest()
                .and()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value())
                .extract().response();
    }

    @Test
    public void historikk_del_cv_transaksjoner() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);

        svarJaPaaDelingAvCv(mockBruker, veileder, aktivitetDTO);

        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(aktivitetDTO.getId(), port, mockBruker, veileder);

        List<AktivitetTransaksjonsType> transaksjoner = aktivitetDTOS.stream().map(AktivitetDTO::getTransaksjonsType).collect(Collectors.toList());

        Assertions.assertThat(transaksjoner).containsOnly(AktivitetTransaksjonsType.OPPRETTET, AktivitetTransaksjonsType.DEL_CV_SVART, AktivitetTransaksjonsType.STATUS_ENDRET);
    }

    private AktivitetDTO skalKunneOppdatereSoknadStatus(MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO) {
        SoknadsstatusDTO body = SoknadsstatusDTO
                .builder()
                .soknadsstatus(Soknadsstatus.VENTER)
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .build();

        AktivitetDTO statusOppdatertRespons = veileder
                .createRequest()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(body)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/soknadStatus?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);

        aktivitetDTO.setStillingFraNavData(aktivitetDTO.getStillingFraNavData().withSoknadsstatus(Soknadsstatus.VENTER));

        assertOppdatertAktivitet(aktivitetDTO, statusOppdatertRespons);

        return statusOppdatertRespons;
    }

    private AktivitetDTO svarJaPaaDelingAvCv(MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO) {
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(true)
                .avtaltDato(AVTALT_DATO)
                .build();
        return veileder
                .createRequest()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);

    }

    private void assertBrukernotifikasjonStoppet(MockBruker mockBruker) {
        Consumer<Nokkel, Done> avroAvroConsumer = testService.createAvroAvroConsumer(brukernotifkasjonFerdigToppik);
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();
        ConsumerRecord<Nokkel, Done> singleRecord = getSingleRecord(avroAvroConsumer, brukernotifkasjonFerdigToppik, 5000);
        assertEquals(mockBruker.getFnr(), singleRecord.value().getFodselsnummer());
        assertEquals(mockBruker.getOppfolgingsPeriode().toString(), singleRecord.value().getGrupperingsId());
    }

    private void assertSentSvarTilRekruteringsbistand(MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO, Consumer<String, DelingAvCvRespons> consumer, boolean svar) {
        // Sjekk at svarmelding sendt til rekrutteringsbistand
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        Svar expectedSvar = Svar.newBuilder()
                .setSvar(svar)
                .setSvartAvBuilder(Ident.newBuilder()
                        .setIdent(veileder.getNavIdent())
                        .setIdentType(IdentTypeEnum.NAV_IDENT))
                // kopier systemgenererte felter
                .setSvarTidspunkt(value.getSvar().getSvarTidspunkt())
                .build();


        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(aktivitetDTO.getStillingFraNavData().getBestillingsId());
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isEqualTo(aktivitetDTO.getId());
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.HAR_SVART);
            assertions.assertThat(value.getSvar()).isEqualTo(expectedSvar);
            assertions.assertAll();
        });
    }

    private void assertAktivitetSvartJa(MockVeileder veileder, AktivitetDTO orginalAktivitet, AktivitetDTO actualAktivitet) {
        CvKanDelesData expectedCvKanDelesData = CvKanDelesData.builder()
                .kanDeles(true)
                .endretAv(veileder.getNavIdent())
                .endretAvType(InnsenderData.NAV)
                .avtaltDato(AVTALT_DATO)
                // kopierer systemgenererte attributter
                .endretTidspunkt(actualAktivitet.getStillingFraNavData().getCvKanDelesData().endretTidspunkt)
                .build();

        AktivitetDTO expectedAktivitet = orginalAktivitet.toBuilder().status(AktivitetStatus.GJENNOMFORES).build();

        StillingFraNavData stillingFraNavData = expectedAktivitet
                .getStillingFraNavData()
                .toBuilder()
                .cvKanDelesData(expectedCvKanDelesData)
                .soknadsstatus(Soknadsstatus.VENTER)
                .livslopsStatus(LivslopsStatus.HAR_SVART)
                .build();

        expectedAktivitet.setStillingFraNavData(stillingFraNavData);

        assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);
    }
}
