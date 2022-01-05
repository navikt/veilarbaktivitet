package no.nav.veilarbaktivitet.oppfolging;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class OppfolgingAvsluttetConsumerTest {

    @Autowired
    KafkaTestService testService;

    @Autowired
    AktivitetTestService testAktivitetservice;

    @Autowired
    JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Autowired
    KafkaTemplate<String, String> producer;

    @Value("${app.kafka.oppfolgingAvsluttetTopic}")
    String oppfolgingAvsluttetTopic;

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
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        MockBruker mockBruker2 = MockNavService.crateHappyBruker();

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO skalIkkeBliHistoriskMockBruker2 = testAktivitetservice.opprettAktivitet(port, mockBruker2, aktivitetDTO);
        AktivitetDTO skalBliHistorisk = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);

        Thread.sleep(10);
        AktivitetDTO skalIkkeBliHistorisk = testAktivitetservice.opprettAktivitet(port, mockBruker, aktivitetDTO);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(skalBliHistorisk.getEndretDato().toInstant(), ZoneId.systemDefault()).plusNanos(5000);
        OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetKafkaDTO = new OppfolgingAvsluttetKafkaDTO()
                .setAktorId(mockBruker.getAktorId())
                .setSluttdato(zonedDateTime);


        SendResult<String, String> sendResult = producer.send(oppfolgingAvsluttetTopic, JsonUtils.toJson(oppfolgingAvsluttetKafkaDTO)).get();
        testService.assertErKonsumertOnprem(oppfolgingAvsluttetTopic, sendResult.getRecordMetadata().offset(), 5);

        List<AktivitetDTO> aktiviteter = testAktivitetservice.hentAktiviteterForFnr(port, mockBruker).aktiviteter;
        AktivitetDTO skalVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalBliHistorisk.getId())).findAny().get();
        AktivitetAssertUtils.assertOppdatertAktivitet(skalBliHistorisk.setHistorisk(true), skalVaereHistorisk);
        assertEquals(AktivitetTransaksjonsType.BLE_HISTORISK, skalVaereHistorisk.getTransaksjonsType());

        AktivitetDTO skalIkkeVaereHistorisk = aktiviteter.stream().filter(a -> a.getId().equals(skalIkkeBliHistorisk.getId())).findAny().get();
        assertEquals(skalIkkeBliHistorisk, skalIkkeVaereHistorisk);

        AktivitetDTO skalIkkeVaereHistoriskMockBruker2 = testAktivitetservice.hentAktiviteterForFnr(port, mockBruker2).aktiviteter.get(0);
        assertEquals(skalIkkeBliHistoriskMockBruker2, skalIkkeVaereHistoriskMockBruker2);
    }
}
