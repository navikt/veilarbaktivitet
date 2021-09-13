package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarbaktivitet.avro.*;
import no.nav.veilarbaktivitet.config.TestAuthContextFilterTingi;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.KontaktInfo;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static no.nav.veilarbaktivitet.testutils.AktivietAssertUtils.assertOppdatertAktivitet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class StillingFraNavControllerITest {

    @Autowired
    KafkaTestService testService;

    @Autowired
    AktivitetTestService testAktivitetService;


    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Value("${topic.inn.stillingFraNav}")
    private String innTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;


    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

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

        AktivitetDTO aktivitetDTO = opprettStillingFraNav(mockBruker);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(true)
                .build();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createConsumer(utTopic);

        Response response = given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilterTingi.identHeder, veileder.getNavIdent())
                .header(TestAuthContextFilterTingi.typeHeder, UserRole.INTERN)
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
                .kanDeles(true)
                .endretAv(veileder.getNavIdent())
                .endretAvType(InnsenderData.NAV)
                // kopierer systemgenererte attributter
                .endretTidspunkt(actualAktivitet.getStillingFraNavData().getCvKanDelesData().endretTidspunkt)
                .build();


        AktivitetDTO expectedAktivitet = aktivitetDTO.toBuilder().status(AktivitetStatus.GJENNOMFORES).build();

        expectedAktivitet.getStillingFraNavData().setCvKanDelesData(expectedCvKanDelesData);

        assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);


        // Sjekk at svarmelding sendt til rekrutteringsbistand
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        Svar expectedSvar = Svar.newBuilder()
                .setSvar(true)
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

    @Test
    public void happy_case_svar_nei() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = opprettStillingFraNav(mockBruker);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(false)
                .build();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createConsumer(utTopic);

        Response response = given()
                .header("Content-type", "application/json")
                .header(TestAuthContextFilterTingi.identHeder, veileder.getNavIdent())
                .header(TestAuthContextFilterTingi.typeHeder, UserRole.INTERN)
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
                // kopierer systemgenererte attributter
                .endretTidspunkt(actualAktivitet.getStillingFraNavData().getCvKanDelesData().endretTidspunkt)
                .build();


        AktivitetDTO expectedAktivitet = aktivitetDTO.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles")
                .build();

        expectedAktivitet.getStillingFraNavData().setCvKanDelesData(expectedCvKanDelesData);

        assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);


        // Sjekk at svarmelding sendt til rekrutteringsbistand
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        Svar expectedSvar = Svar.newBuilder()
                .setSvar(false)
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


    private AktivitetDTO opprettStillingFraNav(MockBruker mockBruker) {
        final Consumer<String, DelingAvCvRespons> consumer = testService.createConsumer(utTopic);

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

        AktivitetsplanDTO aktivitetsplanDTO = testAktivitetService.hentAktiviteterForFnr(port, mockBruker.getFnr());

        assertEquals(1, aktivitetsplanDTO.aktiviteter.size());
        AktivitetDTO aktivitetDTO = aktivitetsplanDTO.getAktiviteter().get(0);

        //TODO skriv bedre test
        assertEquals(AktivitetTypeDTO.STILLING_FRA_NAV, aktivitetDTO.getType());
        assertEquals(melding.getStillingstittel(), aktivitetDTO.getTittel());
        assertEquals("/rekrutteringsbistand/" + melding.getStillingsId(), aktivitetDTO.getLenke());
        assertEquals(melding.getBestillingsId(), aktivitetDTO.getStillingFraNavData().bestillingsId);

        return aktivitetDTO;
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
                .setStillingsId("stillingsId1234")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .setKontaktInfo(KontaktInfo.newBuilder()
                        .setNavn("Jan Saksbehandler")
                        .setTittel("Nav-ansatt")
                        .setEpost("jan.saksbehandler@nav.no")
                        .setMobil("99999999").build())
                .build();
    }

}
