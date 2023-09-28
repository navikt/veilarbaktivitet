package no.nav.veilarbaktivitet.oppfolging.periode;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OppfolgingsperiodeConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${topic.inn.oppfolgingsperiode}")
    String oppfolgingSistePeriodeTopic;

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

    @Test
    void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        SisteOppfolgingsperiodeV1 startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(mockBruker.getAktorId().get())
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, mockBruker.getAktorId().get(), JsonUtils.toJson(startOppfolgiong)).get(1, SECONDS);
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset());


        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.getAktorId()).orElseThrow();

        assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
        assertThat(oppfolgingsperiode.aktorid()).isEqualTo(mockBruker.getAktorId().get());
        assertThat(oppfolgingsperiode.startTid()).isEqualTo(startOppfolgiong.getStartDato());
        assertThat(oppfolgingsperiode.sluttTid()).isNull();

        SisteOppfolgingsperiodeV1 avsluttetOppfolgingsperide = startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(MILLIS));
        SendResult<String, String> avsluttetSendResult = producer.send(oppfolgingSistePeriodeTopic, mockBruker.getAktorId().get(), JsonUtils.toJson(avsluttetOppfolgingsperide)).get(1, SECONDS);
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, avsluttetSendResult.getRecordMetadata().offset());

        Oppfolgingsperiode oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.getAktorId()).orElseThrow();
        assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
        assertThat(oppfolgingsperiodeAvsluttet.aktorid()).isEqualTo(mockBruker.getAktorId().get());
        assertThat(oppfolgingsperiodeAvsluttet.startTid()).isEqualTo(avsluttetOppfolgingsperide.getStartDato());
        assertThat(oppfolgingsperiodeAvsluttet.sluttTid()).isEqualTo(avsluttetOppfolgingsperide.getSluttDato());
    }

    @Test
    void skal_avslutte_aktiviteter_for() throws ExecutionException, InterruptedException {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockBruker mockBruker2 = MockNavService.createHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO skalIkkeBliHistoriskMockBruker2 = aktivitetTestService.opprettAktivitet(mockBruker2, aktivitetDTO);
        AktivitetDTO skalBliHistorisk = aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO);

        UUID oppfolgingsperiodeSkalAvsluttes = mockBruker.getOppfolgingsperiode();
        MockNavService.newOppfolingsperiode(mockBruker);

        AktivitetDTO skalIkkeBliHistorisk = aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO);
        SisteOppfolgingsperiodeV1 avsluttOppfolging = SisteOppfolgingsperiodeV1.builder()
                .uuid(oppfolgingsperiodeSkalAvsluttes)
                .aktorId(mockBruker.getAktorId().get())
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .sluttDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();


        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, JsonUtils.toJson(avsluttOppfolging)).get();
        kafkaTestService.assertErKonsumert(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset());

        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).getAktiviteter();
        AktivitetDTO skalVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalBliHistorisk.getId())).findAny().get();
        AktivitetAssertUtils.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk);
        assertEquals(AktivitetTransaksjonsType.BLE_HISTORISK, skalVaereHistorisk.getTransaksjonsType());

        AktivitetDTO skalIkkeVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalIkkeBliHistorisk.getId())).findAny().get();
        assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk);

        AktivitetDTO skalIkkeVaereHistoriskMockBruker2 = aktivitetTestService.hentAktiviteterForFnr(mockBruker2).getAktiviteter().get(0);
        assertEquals(skalIkkeBliHistoriskMockBruker2, skalIkkeVaereHistoriskMockBruker2);
    }
}
