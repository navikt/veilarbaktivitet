package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.avro.*;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils.assertOppdatertAktivitet;
import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class StillingFraNavControllerITest extends SpringBootTestBase {
    private final String AVTALT_DATO_STRING = "2021-05-04 00:00:00";
    private Date AVTALT_DATO;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @Autowired
    JdbcTemplate jdbc;
    @LocalServerPort
    private int port;
    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    @AfterEach
    void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @BeforeEach
    @SneakyThrows
    void cleanupBetweenTests() {
        AVTALT_DATO = DateUtils.dateFromString(AVTALT_DATO_STRING);
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        DbTestUtils.cleanupTestDb(jdbc);
    }

    @Test
    void happy_case_svar_ja() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        MockVeileder veileder = navMockService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        final var brukernotifikajonOppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = kafkaTestService.createStringAvroConsumer(utTopic);

        AktivitetDTO svartJaPaaDelingAvCv = aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, AVTALT_DATO);

        assertAktivitetSvartJa(veileder, aktivitetDTO, svartJaPaaDelingAvCv);
        assertSentSvarTilRekruteringsbistand(mockBruker, veileder, aktivitetDTO, consumer, true);
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(brukernotifikajonOppgave.getVarselId());

        skalKunneOppdatereSoknadStatus(mockBruker, veileder, svartJaPaaDelingAvCv);
    }

    @Test
    void happy_case_svar_nei() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        MockVeileder veileder = navMockService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        // Kafka consumer for svarmelding til rekrutteringsbistand.
        final Consumer<String, DelingAvCvRespons> consumer = kafkaTestService.createStringAvroConsumer(utTopic);

        AktivitetDTO actualAktivitet = aktivitetTestService.svarPaaDelingAvCv(false, mockBruker, veileder, aktivitetDTO, AVTALT_DATO);

        CvKanDelesData expectedCvKanDelesData = CvKanDelesData.builder()
                .kanDeles(false)
                .endretAv(veileder.getNavIdent())
                .endretAvType(Innsender.NAV)
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
    void svar_naar_frist_utlopt_feiler() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        MockVeileder veileder = navMockService.createVeileder(mockBruker);
        ForesporselOmDelingAvCv foresporselFristUtlopt = AktivitetTestService.createForesporselOmDelingAvCv(UUID.randomUUID().toString(), mockBruker);
        foresporselFristUtlopt.setSvarfrist(Instant.now().minus(2, ChronoUnit.DAYS));
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, foresporselFristUtlopt);
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
    void historikk_del_cv_transaksjoner() {
        MockBruker mockBruker = navMockService.createBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = navMockService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, AVTALT_DATO);

        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(aktivitetDTO.getId(), mockBruker, veileder);

        List<AktivitetTransaksjonsType> transaksjoner = aktivitetDTOS.stream().map(AktivitetDTO::getTransaksjonsType).collect(Collectors.toList());

        Assertions.assertThat(transaksjoner).containsOnly(AktivitetTransaksjonsType.OPPRETTET, AktivitetTransaksjonsType.DEL_CV_SVART, AktivitetTransaksjonsType.STATUS_ENDRET);
    }

    @Test
    void oppdaterKanCvDeles_feilAktivitetstype_feiler() {
        for (AktivitetTypeDTO type : AktivitetTypeDTO.values()) {
            oppdaterKanCvDeles_feilAktivitetstype_feiler(type);
        }
    }

    private void oppdaterKanCvDeles_feilAktivitetstype_feiler(AktivitetTypeDTO typeDTO) {
        if (typeDTO.equals(AktivitetTypeDTO.STILLING_FRA_NAV) || typeDTO.equals(AktivitetTypeDTO.EKSTERNAKTIVITET))
            return;
        MockBruker mockBruker = navMockService.createBruker(BrukerOptions.happyBruker());
        MockVeileder veileder = navMockService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(typeDTO);

        AktivitetDTO oppretetDto = aktivitetTestService.opprettAktivitet(mockBruker, veileder, aktivitetDTO);

        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(oppretetDto.getVersjon()))
                .avtaltDato(AVTALT_DATO)
                .kanDeles(false)
                .build();

        int statusCode = veileder
                .createRequest()
                .and()
                .queryParam("aktivitetId", oppretetDto.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV")
                .then()
                .extract()
                .statusCode();

        assertEquals(HttpStatus.BAD_REQUEST.value(), statusCode, typeDTO.name() + " skal ikke kunne bli svart på");

    }

    @Test
    void oppdaterKanCvDeles_feilVersjon_feiler() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        MockVeileder veileder = navMockService.createVeileder(mockBruker);

        AktivitetDTO orginal = aktivitetTestService.opprettStillingFraNav(mockBruker);
        AktivitetDTO oppdatert = aktivitetTestService.oppdaterAktivitetStatus(mockBruker, veileder, orginal, AktivitetStatus.PLANLAGT);
        DelingAvCvDTO delingAvCvDTO = DelingAvCvDTO.builder()
                .aktivitetVersjon(Long.parseLong(orginal.getVersjon()))
                .avtaltDato(AVTALT_DATO)
                .kanDeles(false)
                .build();

        int statusCode = veileder
                .createRequest()
                .and()
                .param("aktivitetId", oppdatert.getId())
                .body(delingAvCvDTO)
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/stillingFraNav/kanDeleCV?fnr=" + mockBruker.getFnr())
                .then()
                .extract()
                .statusCode();

        assertEquals(statusCode, HttpStatus.CONFLICT.value(), "skal ikke kunne oppdatere aktivitet med feil version");
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
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);

        aktivitetDTO.setStillingFraNavData(aktivitetDTO.getStillingFraNavData().withSoknadsstatus(Soknadsstatus.VENTER));

        assertOppdatertAktivitet(aktivitetDTO, statusOppdatertRespons);

        return statusOppdatertRespons;
    }

    private void assertSentSvarTilRekruteringsbistand(MockBruker mockBruker, MockVeileder veileder, AktivitetDTO aktivitetDTO, Consumer<String, DelingAvCvRespons> consumer, boolean svar) {
        // Sjekk at svarmelding sendt til rekrutteringsbistand
        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
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
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId().get());
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
                .endretAvType(Innsender.NAV)
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
