package no.nav.veilarbaktivitet.veilarbportefolje;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.config.CronService;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class AktiviteterTilPortefoljeServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Autowired
    AktiviteterTilPortefoljeService aktiviteterTilPortefoljeService;

    @Autowired
    CronService cronService;

    @Autowired
    KafkaTestService kafkaTestService;

    @Value("${app.kafka.endringPaaAktivitetTopic}")
    String endringPaaAktivitetTopic;

    Consumer<String, String> consumer;

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);

        consumer = kafkaTestService.createStringStringConsumer(endringPaaAktivitetTopic);
    }

    @Test
    public void skal_sende_meldinger_til_portefolje() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(port, mockBruker, skalSendes);
        cronService.sendMeldingerTilPortefolje();

        ConsumerRecord<String, String> singleRecord = getSingleRecord(consumer, endringPaaAktivitetTopic, 5000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(singleRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), melding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), melding.getVersion().toString());
    }
}
