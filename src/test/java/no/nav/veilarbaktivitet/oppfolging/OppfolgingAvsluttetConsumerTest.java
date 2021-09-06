package no.nav.veilarbaktivitet.oppfolging;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.ITestService;
import no.nav.veilarbaktivitet.util.MockBruker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class OppfolgingAvsluttetConsumerTest {

    @Autowired
    ITestService testService;

    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Autowired
    KafkaTemplate<String, OppfolgingAvsluttetKafkaDTO> producer;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
    }

    @Test
    public void consume() throws ExecutionException, InterruptedException {
        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO aktivitet = testService.opprettAktivitet(port, mockBruker, aktivitetDTO);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(aktivitet.getEndretDato().toInstant(), ZoneId.systemDefault()).plusMinutes(1);
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = new OppfolgingAvsluttetKafkaDTO()
                .setAktorId(aktivitetData.getAktorId())
                .setSluttdato(zonedDateTime);

        producer.send(aktivitetData.getAktorId(), oppfolgingAvsluttetKafkaDTO).get();

        await().atMost(5, SECONDS).until(() -> testService.hentAktiviteterForFnr(port, mockBruker.getFnr()).aktiviteter.stream().filter(AktivitetDTO::isHistorisk).findAny().isPresent());
    }
}