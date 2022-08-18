package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import junit.framework.TestCase;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@DirtiesContext
public class CvDeltITest extends SpringBootTestBase {

    private final ZonedDateTime tidspunkt = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Autowired
    KafkaStringTemplate kafkaStringTemplate;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String innRekrutteringsbistandStatusoppdatering;

    @Autowired
    MeterRegistry meterRegistry;
    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @Before
    public void setUp() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        // meterRegistry.clear gjør at man ikke kan gjenbruke springcontext dersom man skal teste på metrikker
        meterRegistry.clear();
    }

    @Test
    public void behandle_CvDelt_Happy_Case_skal_oppdatere_soknadsstatus_og_lage_metrikk() throws Exception {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        String navIdent = "E271828";

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        Date date = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);

        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });

        Counter counter = meterRegistry.find(StillingFraNavMetrikker.cvDeltMedArbeidsgiver).tag(StillingFraNavMetrikker.suksess, "true").counter();
        TestCase.assertEquals(1.0, counter.count());

        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());
    }

    @Test
    public void duplikat_CvDelt_Skal_ikke_sende_duplikat_brukernotifikasjon() throws Exception {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);
        String navIdent = "E271828";

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        Date date = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);

        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });

        Counter suksessCounter = meterRegistry.find(StillingFraNavMetrikker.cvDeltMedArbeidsgiver).tag(StillingFraNavMetrikker.suksess, "true").counter();
        TestCase.assertEquals(1.0, suksessCounter.count());

        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult2 = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);


        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult2.getRecordMetadata().offset(), 10);

        Counter feilCounter = meterRegistry.find(StillingFraNavMetrikker.cvDeltMedArbeidsgiver).tag(StillingFraNavMetrikker.suksess, "false").counter();
        TestCase.assertEquals(1.0, feilCounter.count());

        brukernotifikasjonAsserts.assertIngenNyeBeskjeder();
    }

    @Test
    public void happy_case_forste_gode_melding_vi_fikk_skal_oppdatere_soknadsstatus_og_lage_metrikk() throws ExecutionException, InterruptedException, TimeoutException {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date date = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);

        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                JsonUtils.fromJson("""
                                {
                                "type":"CV_DELT",
                                "detaljer":"",
                                "utførtAvNavIdent":"Z314159",
                                "tidspunkt":"2022-08-09T14:24:49.124+02:00"
                                }
                                """
                        , RekrutteringsbistandStatusoppdatering.class);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo("Z314159");
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });

        Counter counter = meterRegistry.find(StillingFraNavMetrikker.cvDeltMedArbeidsgiver).tag(StillingFraNavMetrikker.suksess, "true").counter();
        TestCase.assertEquals(1.0, counter.count());

    }

    @Test
    public void hvis_feil_i_json_skal_vi_ikke_endre_aktivitet_og_lage_metrikk() throws ExecutionException, InterruptedException, TimeoutException {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date date = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);

        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        String sendtStatusoppdatering = """
                {
                    "type":"CV_DELT",
                    "detaljer":"",
                    "tidspunkt":2022-08-03T16:31:32.848+02:00
                }
                """;

        SendResult<String, String> sendResult = kafkaStringTemplate.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        Counter counter = meterRegistry.find(StillingFraNavMetrikker.cvDeltMedArbeidsgiver).tag(StillingFraNavMetrikker.suksess, "false").counter();
        TestCase.assertEquals(1.0, counter.count());

        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for);
    }
}
