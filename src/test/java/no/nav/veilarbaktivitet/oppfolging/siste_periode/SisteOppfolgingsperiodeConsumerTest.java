package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class SisteOppfolgingsperiodeConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${topic.inn.sisteOppfolgingsperiode}")
    String oppfolgingSistePeriodeTopic;

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

    @Test
    public void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        SisteOppfolgingsperiodeV1 startOppfolgiong = SisteOppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(mockBruker.getAktorId())
                .startDato(ZonedDateTime.now().minusHours(1).truncatedTo(MILLIS))
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, mockBruker.getAktorId(), JsonUtils.toJson(startOppfolgiong)).get(1, SECONDS);
 //       kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset(), 5);
        await().atMost(5, SECONDS).until(() -> kafkaTestService.erKonsumert(oppfolgingSistePeriodeTopic, "veilarbaktivitet-temp2", sendResult.getRecordMetadata().offset()));


        Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.getAktorId());
        assert oppfolgingsperiode != null;
        Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
        Assertions.assertThat(oppfolgingsperiode.aktorid()).isEqualTo(mockBruker.getAktorId());
        Assertions.assertThat(oppfolgingsperiode.startTid()).isEqualTo(startOppfolgiong.getStartDato());
        Assertions.assertThat(oppfolgingsperiode.sluttTid()).isNull();


        SisteOppfolgingsperiodeV1 avsluttetOppfolgingsperide = startOppfolgiong.withSluttDato(ZonedDateTime.now().truncatedTo(MILLIS));
        SendResult<String, String> avsluttetSendResult = producer.send(oppfolgingSistePeriodeTopic, mockBruker.getAktorId(), JsonUtils.toJson(avsluttetOppfolgingsperide)).get(1, SECONDS);
 //       kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, avsluttetSendResult.getRecordMetadata().offset(), 5);
        await().atMost(5, SECONDS).until(() -> kafkaTestService.erKonsumert(oppfolgingSistePeriodeTopic, "veilarbaktivitet-temp2", avsluttetSendResult.getRecordMetadata().offset()));


        Oppfolgingsperiode oppfolgingsperiodeAvsluttet = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.getAktorId());
        assert oppfolgingsperiodeAvsluttet != null;
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.aktorid()).isEqualTo(mockBruker.getAktorId());
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.startTid()).isEqualTo(avsluttetOppfolgingsperide.getStartDato());
        Assertions.assertThat(oppfolgingsperiodeAvsluttet.sluttTid()).isEqualTo(avsluttetOppfolgingsperide.getSluttDato());
    }
}
