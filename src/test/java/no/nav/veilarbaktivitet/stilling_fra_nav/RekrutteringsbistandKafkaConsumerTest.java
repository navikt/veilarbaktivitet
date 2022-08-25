package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
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
import org.jetbrains.annotations.NotNull;
import org.junit.After;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.FULLFORT;

@DirtiesContext
public class RekrutteringsbistandKafkaConsumerTest extends SpringBootTestBase {


    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String rekrutteringsbistandstatusoppdateringtopic;

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Autowired
    KafkaStringTemplate kafkaStringTemplate;

    @Autowired
    RekrutteringsbistandStatusoppdateringService rekrutteringsbistandStatusoppdateringService;

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
        aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.opprettStillingFraNav(mockBruker);
        bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;
    }

    @After
    public void tearDown() {

    }

    @Test
    public void behandle_CvDelt_Happy_Case_skal_oppdatere_soknadsstatus_og_lage_metrikk() throws Exception {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, INGEN_DETALJER, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretDato())
                    .as("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                    .isNotEqualTo(sendtStatusoppdatering.tidspunkt());
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0
        ));

        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());
    }

    @Test
    public void behandle_ikke_fatt_jobben() throws Exception {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        String ikkeFattJobbenDetaljer = """
                Vi har sett nærmere på CV-en din, dessverre passer ikke kompetansen din helt med behovene til arbeidsgiveren.
                Derfor deler vi ikke CV-en din med arbeidsgiveren denne gangen. Lykke til videre med jobbsøkingen.
                """;
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN, ikkeFattJobbenDetaljer, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);

        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretDato())
                    .as("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                    .isNotEqualTo(sendtStatusoppdatering.tidspunkt());
            assertions.assertThat(aktivitetData_etter.getVersjon()).as("Forventer ny versjon av aktivitet").isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(FULLFORT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.FIKK_IKKE_JOBBEN);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getIkkefattjobbendetaljer()).isEqualTo(ikkeFattJobbenDetaljer);

            assertions.assertAll();
        });

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0
        ));

        // TODO Trello-oppgave "Ny brukernotifikasjon_Ikke fått jobben"
//        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());
    }

    @Test
    public void behandle_ikke_fatt_jobben_etter_cv_delt() throws Exception {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, INGEN_DETALJER, navIdent, tidspunkt);
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);
        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0
        ));

        String ikkeFattJobbenDetaljer = """
            Vi har fått beskjed om at arbeidsgiveren har ansatt en person. Dessverre var det ikke deg denne gangen.
            Ansettelsesprosessen er ferdig. Lykke til videre med jobbsøkingen.
        """;
        RekrutteringsbistandStatusoppdatering sendtStatusoppdateringFikkIkkeJobben =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN, ikkeFattJobbenDetaljer, navIdent, tidspunkt);
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResultFikkIkkeJobben = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdateringFikkIkkeJobben).get(5, TimeUnit.SECONDS);
        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResultFikkIkkeJobben.getRecordMetadata().offset(), 10);
        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretDato())
                    .as("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                    .isNotEqualTo(sendtStatusoppdatering.tidspunkt());
            assertions.assertThat(aktivitetData_etter.getVersjon()).as("Forventer ny versjon av aktivitet").isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(FULLFORT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.FIKK_IKKE_JOBBEN);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getIkkefattjobbendetaljer()).isEqualTo(ikkeFattJobbenDetaljer);

            assertions.assertAll();
        });

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 2.0
        ));

        // TODO Trello-oppgave "Ny brukernotifikasjon_Ikke fått jobben"
//        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());
    }

    @Test
    public void behandle_CvDelt_svart_nei_skal_oppdatere_soknadsstatus_og_lage_metrikk() throws Exception {
        aktivitetTestService.svarPaaDelingAvCv(Boolean.FALSE, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);
        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for);
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Aktivitet AVBRUTT", 1.0
        ));
    }

    @Test
    public void duplikat_CvDelt_Skal_ikke_sende_duplikat_brukernotifikasjon() throws Exception {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, INGEN_DETALJER, navIdent, tidspunkt);

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

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0
        ));
        brukernotifikasjonAsserts.assertBeskjedSendt(mockBruker.getFnrAsFnr());

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult2 = navCommonJsonProducerFactory.send(innRekrutteringsbistandStatusoppdatering, bestillingsId, sendtStatusoppdatering).get(5, TimeUnit.SECONDS);
        kafkaTestService.assertErKonsumertAiven(innRekrutteringsbistandStatusoppdatering, sendResult2.getRecordMetadata().offset(), 10);

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0,
                "Allerede delt", 1.0
        ));

        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId())).isEqualTo(aktivitetData_etter);

        brukernotifikasjonAsserts.assertIngenNyeBeskjeder();
    }

    @Test
    public void happy_case_forste_gode_melding_vi_fikk_skal_oppdatere_soknadsstatus_og_lage_metrikk() throws ExecutionException, InterruptedException, TimeoutException {

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

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                SUKSESS, 1.0
        ));

    }
    @Test
    public void hvis_feil_i_json_skal_vi_ikke_endre_aktivitet_og_lage_metrikk() throws ExecutionException, InterruptedException, TimeoutException {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
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

        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Ugyldig melding", 1.0
        ));

        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for);
    }
    @Test
    public void cvdelt_hvis_ikke_svart() throws ExecutionException, InterruptedException, TimeoutException {
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 10);

        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId())).isEqualTo(aktivitetData_for);
        Assertions.assertThat(antallAvHverArsak())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "Ikke svart", 1.0
                ));
    }

    @Test
    public void cvdelt_hvis_aktivitet_ikke_finnes() throws ExecutionException, InterruptedException, TimeoutException {
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        "666",
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 10);

        Assertions.assertThat(antallAvHverArsak())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "Bestillingsid ikke funnet", 1.0
                ));
    }

    @Test
    public void cvdelt_hvis_NEI_pa_deling_av_cv() throws ExecutionException, InterruptedException, TimeoutException {
        aktivitetTestService.svarPaaDelingAvCv(NEI, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        aktivitetDTO.getStillingFraNavData().bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 10);

        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId())).isEqualTo(aktivitetData_for);
        Assertions.assertThat(antallAvHverArsak())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "Aktivitet AVBRUTT", 1.0
                ));
    }

    @Test
    public void cvdelt_aktivitet_er_i_status_FULLFORT() throws ExecutionException, InterruptedException, TimeoutException {
        AktivitetDTO aktivitetDTO_svartJA = aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));
        aktivitetTestService.oppdatterAktivitetStatus(mockBruker, veileder, aktivitetDTO_svartJA, FULLFORT);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        aktivitetDTO.getStillingFraNavData().bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 10);

        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretDato())
                    .as("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                    .isNotEqualTo(sendtStatusoppdatering.tidspunkt());
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isEqualTo(InnsenderData.NAV.name());
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });

        Assertions.assertThat(antallAvHverArsak())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "", 1.0
                ));
    }
    @Test
    public void cvdelt_aktivitet_er_i_status_AVBRUTT() throws ExecutionException, InterruptedException, TimeoutException {
        AktivitetDTO aktivitetDTO_svartJA = aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));
        aktivitetTestService.oppdatterAktivitetStatus(mockBruker, veileder, aktivitetDTO_svartJA, AktivitetStatus.AVBRUTT);
        AktivitetDTO aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        aktivitetDTO.getStillingFraNavData().bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 10);
        AktivitetDTO aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.getId());

        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for);
        Assertions.assertThat(antallAvHverArsak())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "Aktivitet AVBRUTT", 1.0
                ));
    }
    @NotNull
    private Map<String, Double> antallAvHverArsak() {
        return meterRegistry.find(StillingFraNavMetrikker.REKRUTTERINGSBISTANDSTATUSOPPDATERING).counters().stream()
                .collect(Collectors.toMap(c -> c.getId().getTag("reason"), Counter::count, Double::sum));
    }
    private final String DETALJER_TEKST = "";
    private final boolean JA = Boolean.TRUE;
    private final boolean NEI = Boolean.FALSE;
    private final ZonedDateTime tidspunkt = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());
    private final String navIdent = "E271828";
    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder veileder = MockNavService.createVeileder(mockBruker);
    private final Date date = Date.from(Instant.ofEpochSecond(1));
    private final String SUKSESS = "";
    private final String INGEN_DETALJER = "";
    private AktivitetDTO aktivitetDTO;
    private String bestillingsId;
}
