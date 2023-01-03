package no.nav.veilarbaktivitet.veilarbportefolje;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.junit.Assert.*;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

public class AktiviteterTilKafkaAivenServiceTest extends SpringBootTestBase {

    @Autowired
    StillingFraNavTestService stillingFraNavTestService;

    @Autowired
    AktiviteterTilKafkaService cronService;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    UnleashClient unleashClient;

    @Value("${topic.ut.portefolje}")
    String portefoljeTopic;

    @Value("${topic.ut.aktivitetdata.rawjson}")
    String aktivitetRawJson;

    Consumer<String, String> protefoljeConsumer;

    Consumer<String, AktivitetData> aktivterKafkaConsumer;

    @Autowired
    KafkaStringTemplate portefoljeProducer;

    @Autowired
    LockProvider lockProvider;

    @BeforeEach
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();

        protefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljeTopic);
        aktivterKafkaConsumer = kafkaTestService.createStringJsonConsumer(aktivitetRawJson);

        Mockito.reset(portefoljeProducer);
    }

    @Test
    public void skal_sende_meldinger_til_portefolje() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, MockNavService.createVeileder(mockBruker), skalSendes);
        cronService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(protefoljeConsumer, portefoljeTopic, 10000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), melding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), melding.getVersion().toString());

        ConsumerRecord<String, AktivitetData> singleRecord = getSingleRecord(aktivterKafkaConsumer, aktivitetRawJson, 10000);
        AktivitetData aktivitetMelding = singleRecord.value();

        assertEquals(opprettetAktivitet, AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetMelding, false));

        cronService.sendOppTil5000AktiviterTilPortefolje();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, protefoljeConsumer));
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(aktivitetRawJson, aktivterKafkaConsumer));
    }

    @Test
    public void skal_committe_hver_melding() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData1 = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetData aktivitetData2 = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes1 = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData1, false);
        AktivitetDTO skalSendes2 = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData2, false);

        aktivitetTestService.opprettAktivitet(mockBruker, MockNavService.createVeileder(mockBruker), skalSendes1);
        aktivitetTestService.opprettAktivitet(mockBruker, MockNavService.createVeileder(mockBruker), skalSendes2);


        Mockito
                .doCallRealMethod()
                .doThrow(IllegalStateException.class)
                .when(portefoljeProducer)
                .send((ProducerRecord<String, String>) Mockito.any(ProducerRecord.class));


        assertThrows(IllegalStateException.class, () -> cronService.sendOppTil5000AktiviterTilPortefolje());

        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AKTIVITET WHERE AKTIVITET.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NOT NULL", Long.class)).isEqualTo(1);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AKTIVITET WHERE AKTIVITET.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL", Long.class)).isEqualTo(1);

    }
}
