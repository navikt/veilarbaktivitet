package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.ArgumentMatchers.any;

public class CvDeltServiceTest extends SpringBootTestBase {

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String rekrutteringsbistandstatusoppdateringtopic;

    @Autowired
    DelingAvCvDAO delingAvCvDAO;

    @SpyBean
    CvDeltService cvDeltService;

    @Autowired
    AktivitetDAO aktivitetDAO;

    private final String navIdent = "P314159";
    private final String DETALJER_TEKST = "";
    private final boolean JA = Boolean.TRUE;
    private final boolean NEI = Boolean.FALSE;
    private final ZonedDateTime tidspunkt = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault());

    @Test
    public void happy_case_svart_ja() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));
        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData_for = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        cvDeltService.behandleRekrutteringsbistandoppdatering(bestillingsId, RekrutteringsbistandStatusoppdateringEventType.CV_DELT, navIdent, aktivitetData_for);

        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_for.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_for.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_for.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_meldingen_mangler_navident_blir_navIdent_satt_til_SYSTEM() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, null, tidspunkt);

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;
        AktivitetData aktivitetData_for = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        cvDeltService.behandleRekrutteringsbistandoppdatering(bestillingsId, sendtStatusoppdatering.type(), null, aktivitetData_for);

        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo("SYSTEM");
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_bruker_ikke_har_samtykket_om_deling_av_cv_skal_likevel_aktivitet_oppdateres() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, null, tidspunkt);

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData_for = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        cvDeltService.behandleRekrutteringsbistandoppdatering(bestillingsId, sendtStatusoppdatering.type(), navIdent, aktivitetData_for);

        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().cvKanDelesData).isNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_NEI_pa_deling_av_cv_oppdateres_soknadsstatus_pa_aktivitet() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(NEI, mockBruker, veileder, aktivitetDTO, tidspunkt);

        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData_for =
                aktivitetDAO.hentAktiviteterForAktorId(mockBruker.getAktorIdAsAktorId()).stream()
                        .sorted(comparingLong(AktivitetData::getVersjon))
                        .reduce((a, b) -> b)
                        .orElseThrow();

        cvDeltService.behandleRekrutteringsbistandoppdatering(
                bestillingsId,
                RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
                navIdent,
                aktivitetData_for
        );

        AktivitetData aktivitetData_etter = aktivitetDAO.hentAktiviteterForAktorId(mockBruker.getAktorIdAsAktorId()).stream()
                .sorted(comparingLong(AktivitetData::getVersjon))
                .reduce((a, b) -> b)
                .orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(AktivitetStatus.AVBRUTT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    @Test
    public void hvis_type_er_ikke_fatt_jobben_gir_vi_feilmelding_om_at_det_ikke_er_ferdig_enna() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData_for = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        Assertions.assertThatThrownBy(() -> cvDeltService.behandleRekrutteringsbistandoppdatering(
                bestillingsId,
                RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN,
                navIdent,
                aktivitetData_for
        )).isInstanceOf(org.apache.commons.lang3.NotImplementedException.class);
    }

    @Test
    public void consumertest_behandleRekrutteringsbistandoppdatering_kalles() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);
        String bestillingsId = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).orElseThrow();

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);

        Mockito.verify(cvDeltService, Mockito.times(1)
                        .description("Consumeren skal kalle metoden behandleRekrutteringsbistandoppdatering med bestillingsId, type og navIdent fra kafkamelding"))
                .behandleRekrutteringsbistandoppdatering(bestillingsId, RekrutteringsbistandStatusoppdateringEventType.CV_DELT, navIdent, aktivitetData);
    }

    @Test
    public void consumertest_hvis_aktivitet_ikke_finnes_kalles_ikke_behandleRekrutteringsbistandoppdatering() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        aktivitetTestService.opprettStillingFraNav(mockBruker);

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        "666",
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);

        Mockito.verify(cvDeltService, Mockito.never().description("Consumeren skal ikke kalle metoden behandleRekrutteringsbistandoppdatering når vi ikke finner aktivitet"))
                .behandleRekrutteringsbistandoppdatering(any(String.class), any(RekrutteringsbistandStatusoppdateringEventType.class), any(String.class), any(AktivitetData.class));
    }

    @Test
    public void consumertest_hvis_NEI_pa_deling_av_cv_kalles_ikke_behandleRekrutteringsbistandoppdatering() throws ExecutionException, InterruptedException, TimeoutException {

        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.svarPaaDelingAvCv(NEI, mockBruker, veileder, aktivitetDTO, Date.from(Instant.ofEpochSecond(1)));

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering = new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, DETALJER_TEKST, navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult =
                navCommonJsonProducerFactory.send(
                        rekrutteringsbistandstatusoppdateringtopic,
                        aktivitetDTO.getStillingFraNavData().bestillingsId,
                        sendtStatusoppdatering
                ).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);

        Mockito.verify(cvDeltService, Mockito.never().description("Consumeren skal ikke kalle metoden behandleRekrutteringsbistandoppdatering fordi aktivitet er AVBRUTT"))
                .behandleRekrutteringsbistandoppdatering(any(String.class), any(RekrutteringsbistandStatusoppdateringEventType.class), any(String.class), any(AktivitetData.class));
    }
}