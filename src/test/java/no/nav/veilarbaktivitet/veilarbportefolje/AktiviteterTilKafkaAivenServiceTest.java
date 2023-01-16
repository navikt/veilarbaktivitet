package no.nav.veilarbaktivitet.veilarbportefolje;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.ArenaMeldingHeaders;
import no.nav.veilarbaktivitet.aktivitetskort.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavTestService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetskortTestBuilder;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class AktiviteterTilKafkaAivenServiceTest extends SpringBootTestBase {

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

    Consumer<String, String> portefoljeConsumer;

    Consumer<String, AktivitetData> aktiviteterKafkaConsumer;

    @Autowired
    KafkaStringTemplate portefoljeProducer;

    @Autowired
    LockProvider lockProvider;

    @BeforeEach
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);

        JdbcTemplateLockProvider l = (JdbcTemplateLockProvider) lockProvider;
        l.clearCache();

        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljeTopic);
        aktiviteterKafkaConsumer = kafkaTestService.createStringJsonConsumer(aktivitetRawJson);

        Mockito.reset(portefoljeProducer);
    }

    @Test
    void skal_sende_meldinger_til_portefolje() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, MockNavService.createVeileder(mockBruker), skalSendes);
        cronService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, 10000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), melding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), melding.getVersion().toString());

        ConsumerRecord<String, AktivitetData> singleRecord = getSingleRecord(aktiviteterKafkaConsumer, aktivitetRawJson, 10000);
        AktivitetData aktivitetMelding = singleRecord.value();

        assertEquals(opprettetAktivitet, AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetMelding, false));


        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(aktivitetRawJson, aktiviteterKafkaConsumer));
    }

    @Test
    void skal_sende_tiltak_til_portefolje() throws InterruptedException {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        UUID funksjonellId = UUID.randomUUID();

        String arenaTiltakskode = KafkaAktivitetDAO.TILTAKSKODE_VARIG_LONNSTILSKUDD;

        ArenaId arenaId = new ArenaId("ARENATA123");

        Aktivitetskort actual = AktivitetskortTestBuilder.ny(funksjonellId, AktivitetStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);

        ArenaMeldingHeaders kontekst = new ArenaMeldingHeaders(arenaId, arenaTiltakskode);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst));

        Thread.sleep(2000L);

        cronService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, 10000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        Assertions.assertThat(melding.getTiltakskode()).isEqualTo(arenaTiltakskode);
        Assertions.assertThat(melding.getAktivitetId()).isEqualTo(arenaId.id());
        Assertions.assertThat(melding.getAktivitetType()).isEqualTo(AktivitetTypeDTO.TILTAK);

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_sende_nye_lonnstilskudd_til_portefolje() {
        MockBruker mockBruker = MockNavService.createHappyBruker();

        UUID funksjonellId = UUID.randomUUID();


        Aktivitetskort aktivitetskort = AktivitetskortTestBuilder.ny(funksjonellId, AktivitetStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);

        KafkaAktivitetskortWrapperDTO wrapper = AktivitetskortTestBuilder.aktivitetskortMelding(aktivitetskort, funksjonellId, "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);

        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(wrapper));


        cronService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, 10000);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        Assertions.assertThat(melding.getTiltakskode()).isEqualTo(KafkaAktivitetDAO.TILTAKSKODE_MIDLERTIDIG_LONNSTILSKUDD);
        Assertions.assertThat(melding.getAktivitetType()).isEqualTo(AktivitetTypeDTO.TILTAK);


        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_committe_hver_melding() {
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
