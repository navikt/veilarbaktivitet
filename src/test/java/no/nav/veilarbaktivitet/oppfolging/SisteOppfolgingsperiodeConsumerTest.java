package no.nav.veilarbaktivitet.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import static org.awaitility.Awaitility.await;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class SisteOppfolgingsperiodeConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${topic.inn.sisteOppfolgingsperiode}")
    String oppfolgingSistePeriodeTopic;

    @Test
    public void skal_opprette_siste_oppfolgingsperiode() throws InterruptedException, ExecutionException, TimeoutException {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = SisteOppfolgingsperiodeV1.builder()
                .uuid(mockBruker.getOppfolgingsperiode())
                .aktorId(mockBruker.getAktorId())
                .startDato(ZonedDateTime.now())
                .build();
        SendResult<String, String> sendResult = producer.send(oppfolgingSistePeriodeTopic, mockBruker.getAktorId(), JsonUtils.toJson(sisteOppfolgingsperiodeV1)).get(1, SECONDS);
        kafkaTestService.assertErKonsumertAiven(oppfolgingSistePeriodeTopic, sendResult.getRecordMetadata().offset(), 5);
        record SisteOppfolgingsperiode(UUID oppfolgingsperiode, String aktorId, ZonedDateTime startDato, ZonedDateTime sluttDato) {}
        await().atMost(5, SECONDS).until(() -> {
            try {
                SisteOppfolgingsperiode sisteOppfolgingsperiode = jdbc.queryForObject("SELECT * FROM siste_oppfolgingsperiode WHERE aktor_id=?", SisteOppfolgingsperiode.class, mockBruker.getAktorId());
                assert sisteOppfolgingsperiode != null;
                Assertions.assertThat(sisteOppfolgingsperiode.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
                Assertions.assertThat(sisteOppfolgingsperiode.aktorId()).isEqualTo(mockBruker.getAktorId());
                Assertions.assertThat(sisteOppfolgingsperiode.startDato()).isEqualTo(sisteOppfolgingsperiodeV1.getStartDato());
                Assertions.assertThat(sisteOppfolgingsperiode.sluttDato()).isNull();

                return true;
            } catch (EmptyResultDataAccessException error) {
                return false;
            }
        });
    }
}
