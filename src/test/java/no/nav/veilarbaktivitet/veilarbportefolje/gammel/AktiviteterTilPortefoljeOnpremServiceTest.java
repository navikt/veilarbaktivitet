package no.nav.veilarbaktivitet.veilarbportefolje.gammel;

import no.nav.common.featuretoggle.UnleashClient;
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
import no.nav.veilarbaktivitet.veilarbportefolje.KafkaAktivitetMeldingV4;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class AktiviteterTilPortefoljeOnpremServiceTest {

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

    @Autowired
    UnleashClient unleashClient;

    @Value("${app.kafka.endringPaaAktivitetTopic}")
    String endringPaaAktivitetTopic;

    @Value("${topic.ut.portefolje}")
    String portefoljeTopic;

    @Value("${topic.ut.aktivitetdata.rawjson}")
    String aktivitetRawJson;


    Consumer<String, String> onpremPortefoljeConsumer;

    Consumer<String, String> protefoljeConsumer;

    Consumer<String, AktivitetData> aktivterKafkaConsumer;

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);

        Mockito.when(unleashClient.isEnabled(CronService.STOPP_AKTIVITETER_TIL_KAFKA)).thenReturn(false);

        onpremPortefoljeConsumer = kafkaTestService.createStringStringConsumer(endringPaaAktivitetTopic);
        protefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljeTopic);
        aktivterKafkaConsumer = kafkaTestService.createStringJsonConsumer(aktivitetRawJson);
    }

    @Test
    public void skal_sende_meldinger_til_portefolje() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(port, mockBruker, skalSendes);

        cronService.sendMeldingerTilPortefoljeOnprem();

        ConsumerRecord<String, String> singleRecord = getSingleRecord(onpremPortefoljeConsumer, endringPaaAktivitetTopic, 5000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(singleRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), melding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), melding.getVersion().toString());
    }

    @Test
    public void skal_fugere_sammen_med_aiven() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(port, mockBruker, MockNavService.createVeileder(mockBruker), skalSendes);

        //onprem
        cronService.sendMeldingerTilPortefoljeOnprem();

        ConsumerRecord<String, String> onpremSingleRecord = getSingleRecord(onpremPortefoljeConsumer, endringPaaAktivitetTopic, 5000);
        KafkaAktivitetMeldingV4 onpremMelding = JsonUtils.fromJson(onpremSingleRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), onpremMelding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), onpremMelding.getVersion().toString());


        //aiven
        cronService.sendMeldingerTilPortefoljeAiven();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(protefoljeConsumer, portefoljeTopic, 5000);
        KafkaAktivitetMeldingV4 aivenMelding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), aivenMelding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), aivenMelding.getVersion().toString());

        ConsumerRecord<String, AktivitetData> singleRecord = getSingleRecord(aktivterKafkaConsumer, aktivitetRawJson, 5000);
        AktivitetData aktivitetMelding = singleRecord.value();

        assertEquals(opprettetAktivitet, AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetMelding, false));


        //skal ikke sendes på nytt
        cronService.sendMeldingerTilPortefoljeOnprem();
        cronService.sendMeldingerTilPortefoljeAiven();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, onpremPortefoljeConsumer));
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(endringPaaAktivitetTopic, protefoljeConsumer));
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(aktivitetRawJson, aktivterKafkaConsumer));
    }
}
