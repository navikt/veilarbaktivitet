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
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class CvDeltServiceTest extends SpringBootTestBase {

    @Autowired
    KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering> navCommonJsonProducerFactory;

    @Value("${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private String rekrutteringsbistandstatusoppdateringtopic;

    @Autowired
    DelingAvCvDAO delingAvCvDAO;

    @Autowired
    CvDeltService cvDeltService;

    @Autowired
    AktivitetDAO aktivitetDAO;

    private final boolean JA = Boolean.TRUE;
    private final boolean NEI = Boolean.FALSE;

    @Test
    public void happy_case_svart_ja() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, tidspunkt);
        String key = aktivitetDTO.getStillingFraNavData().bestillingsId;

        AktivitetData aktivitetData_før = delingAvCvDAO.hentAktivitetMedBestillingsId(key).orElseThrow();

        String navIdent = "P314159";
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt);

        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(rekrutteringsbistandstatusoppdateringtopic, key, sendtStatusoppdatering).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);
        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(key).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getVersjon()).isGreaterThan(aktivitetData_før.getVersjon());
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo(navIdent);
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(aktivitetData_før.getStatus());
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getLivslopsStatus()).isSameAs(aktivitetData_før.getStillingFraNavData().getLivslopsStatus());
            assertions.assertAll();
        });
    }

    @Test
    public void feil_json_mangler_navident() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(JA, mockBruker, veileder, aktivitetDTO, tidspunkt);

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", null, tidspunkt);

        String key = aktivitetDTO.getStillingFraNavData().bestillingsId;
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(rekrutteringsbistandstatusoppdateringtopic, key, sendtStatusoppdatering).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);
        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(key).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getEndretAv()).isEqualTo("SYSTEM");
            assertions.assertThat(aktivitetData_etter.getLagtInnAv()).isSameAs(InnsenderData.NAV);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    @Test
    public void cverdelt_selv_om_bruker_ikke_har_samtykket_om_deling_av_cv() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));

        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", null, tidspunkt);

        String key = aktivitetDTO.getStillingFraNavData().bestillingsId;
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(rekrutteringsbistandstatusoppdateringtopic, key, sendtStatusoppdatering).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);
        AktivitetData aktivitetData_etter = delingAvCvDAO.hentAktivitetMedBestillingsId(key).orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData()).isNotNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().cvKanDelesData).isNull();
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isSameAs(Soknadsstatus.CV_DELT);
            assertions.assertAll();
        });
    }

    @Test
    public void ingenting_skjer_nar_cverdelt_selv_om_bruker_har_svart_NEI_pa_deling_av_cv() throws ExecutionException, InterruptedException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        Date tidspunkt = Date.from(Instant.ofEpochSecond(1));
        aktivitetTestService.svarPaaDelingAvCv(NEI, mockBruker, veileder, aktivitetDTO, tidspunkt);

        String navIdent = "P314159";
        RekrutteringsbistandStatusoppdatering sendtStatusoppdatering =
                new RekrutteringsbistandStatusoppdatering(RekrutteringsbistandStatusoppdateringEventType.CV_DELT, "", navIdent, tidspunkt);

        String key = aktivitetDTO.getStillingFraNavData().bestillingsId;
        SendResult<String, RekrutteringsbistandStatusoppdatering> sendResult = navCommonJsonProducerFactory.send(rekrutteringsbistandstatusoppdateringtopic, key, sendtStatusoppdatering).get(1, SECONDS);

        kafkaTestService.assertErKonsumertAiven(rekrutteringsbistandstatusoppdateringtopic, sendResult.getRecordMetadata().offset(), 5);

        AktivitetData aktivitetData_etter = aktivitetDAO.hentAktiviteterForAktorId(mockBruker.getAktorIdAsAktorId())
                .stream().sorted(Comparator.comparingLong(AktivitetData::getId))
                .reduce((a, b) -> b)
                .orElseThrow();

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(aktivitetData_etter.getStatus()).isSameAs(AktivitetStatus.AVBRUTT);
            assertions.assertThat(aktivitetData_etter.getStillingFraNavData().getSoknadsstatus()).isNull();
            assertions.assertAll();
        });
    }
}