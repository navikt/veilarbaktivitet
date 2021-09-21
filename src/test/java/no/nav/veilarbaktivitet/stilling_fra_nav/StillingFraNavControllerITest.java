package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.*;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.InnsenderData;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
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

import java.util.List;
import java.util.stream.Collectors;

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
    AktivitetTestService aktivitetTestService;

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

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(true)
                .build();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createConsumer(utTopic);

        Response response = veileder
                .createRequest()
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

        AktivitetAssertUtils.assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);


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
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(false)
                .build();

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = testService.createConsumer(utTopic);

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
                // kopierer systemgenererte attributter
                .endretTidspunkt(actualAktivitet.getStillingFraNavData().getCvKanDelesData().endretTidspunkt)
                .build();


        AktivitetDTO expectedAktivitet = aktivitetDTO.toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .avsluttetKommentar("Automatisk avsluttet fordi cv ikke skal deles")
                .build();

        expectedAktivitet.getStillingFraNavData().setCvKanDelesData(expectedCvKanDelesData);

        AktivitetAssertUtils.assertOppdatertAktivitet(expectedAktivitet, actualAktivitet);


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

    @Test
    public void historikk_del_cv_transaksjoner() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(aktivitetDTO.getVersjon()))
                .kanDeles(true)
                .build();

        veileder
                .createRequest()
                .param("aktivitetId", aktivitetDTO.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value());

        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(aktivitetDTO.getId(), port, mockBruker, veileder);

        List<AktivitetTransaksjonsType> transaksjoner = aktivitetDTOS.stream().map(AktivitetDTO::getTransaksjonsType).collect(Collectors.toList());

        Assertions.assertThat(transaksjoner).contains(AktivitetTransaksjonsType.OPPRETTET, AktivitetTransaksjonsType.DEL_CV_SVART, AktivitetTransaksjonsType.STATUS_ENDRET);
    }

}
