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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;
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

    @Autowired
    private SistePeriodeDAO sistePeriodeDAO;

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


        await().atMost(5, SECONDS).until(() -> {
            try {
                Oppfolgingsperiode oppfolgingsperiode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(mockBruker.getAktorId());
                assert oppfolgingsperiode != null;
                Assertions.assertThat(oppfolgingsperiode.oppfolgingsperiode()).isEqualTo(mockBruker.getOppfolgingsperiode());
                Assertions.assertThat(oppfolgingsperiode.aktorid()).isEqualTo(mockBruker.getAktorId());
                Assertions.assertThat(oppfolgingsperiode.startTid()).isEqualTo(sisteOppfolgingsperiodeV1.getStartDato());
                Assertions.assertThat(oppfolgingsperiode.sluttTid()).isNull();

                return true;
            } catch (EmptyResultDataAccessException error) {
                return false;
            }
        });
    }
}
