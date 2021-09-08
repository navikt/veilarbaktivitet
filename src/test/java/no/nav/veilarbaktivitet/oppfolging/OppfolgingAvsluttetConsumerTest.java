package no.nav.veilarbaktivitet.oppfolging;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
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
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
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
    KafkaTemplate<String, String> producer;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
    }

    @Test
    @SuppressWarnings("java:S2925")
    public void skal_avslutte_aktiviteter_for() throws ExecutionException, InterruptedException {
        MockBruker mockBruker = MockBruker.happyBruker();
        MockBruker mockBruker2 = MockBruker.happyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO skalIkkeBliHistoriskMockBruker2 = testService.opprettAktivitet(port, mockBruker2, aktivitetDTO);
        AktivitetDTO skalBliHistorisk = testService.opprettAktivitet(port, mockBruker, aktivitetDTO);

        Thread.sleep(10);
        AktivitetDTO skalIkkeBliHistorisk = testService.opprettAktivitet(port, mockBruker, aktivitetDTO);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(skalBliHistorisk.getEndretDato().toInstant(), ZoneId.systemDefault()).plusNanos(5000);
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = new OppfolgingAvsluttetKafkaDTO()
                .setAktorId(mockBruker.getAktorId())
                .setSluttdato(zonedDateTime);

        SendResult<String, String> oppfolgingAvsluttetTopic = producer.send("oppfolgingAvsluttetTopic", JsonUtils.toJson(oppfolgingAvsluttetKafkaDTO)).get();
        await().atMost(5, SECONDS).until(() -> testService.erKonsumert("oppfolgingAvsluttetTopic", "veilarbaktivitet-consumer", oppfolgingAvsluttetTopic.getRecordMetadata().offset()));

        List<AktivitetDTO> aktiviteter = testService.hentAktiviteterForFnr(port, mockBruker.getFnr()).aktiviteter;
        AktivitetDTO skalVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalBliHistorisk.getId())).findAny().get();
        testService.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk);
        assertEquals(AktivitetTransaksjonsType.BLE_HISTORISK, skalVaereHistorisk.getTransaksjonsType());

        AktivitetDTO skalIkkeVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalIkkeBliHistorisk.getId())).findAny().get();
        assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk);

        AktivitetDTO skalIkkeVaereHistoriskMockBruker2 = testService.hentAktiviteterForFnr(port, mockBruker2.getFnr()).aktiviteter.get(0);
        assertEquals(skalIkkeBliHistoriskMockBruker2, skalIkkeVaereHistoriskMockBruker2);
    }
}
