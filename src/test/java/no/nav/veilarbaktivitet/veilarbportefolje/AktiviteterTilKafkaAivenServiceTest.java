package no.nav.veilarbaktivitet.veilarbportefolje;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortUtil;
import no.nav.veilarbaktivitet.aktivitetskort.ArenaKort;
import no.nav.veilarbaktivitet.aktivitetskort.ArenaMeldingHeaders;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
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

import static no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD;
import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class AktiviteterTilKafkaAivenServiceTest extends SpringBootTestBase {

    @Autowired
    AktiviteterTilKafkaService cronService;

    @Autowired
    KafkaTestService kafkaTestService;

    @Value("${topic.ut.portefolje}")
    String portefoljeTopic;

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

        Mockito.reset(portefoljeProducer);
    }

    @Test
    void skal_sende_meldinger_til_portefolje() {
        MockBruker mockBruker = navMockService.createBruker(BrukerOptions.happyBruker());
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, navMockService.createVeileder(mockBruker), skalSendes);
        cronService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        assertEquals(opprettetAktivitet.getId(), melding.getAktivitetId());
        assertEquals(opprettetAktivitet.getVersjon(), melding.getVersion().toString());

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_ikke_sende_arena_tiltak_til_portefolje() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        Aktivitetskort actual = AktivitetskortUtil.ny(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);
        KafkaAktivitetskortWrapperDTO wrapperDTO = new KafkaAktivitetskortWrapperDTO(
                AktivitetskortType.ARENA_TILTAK,
                actual,
                "source",
                UUID.randomUUID()
                );
        aktivitetTestService.opprettEksterntArenaKort(new ArenaKort(wrapperDTO, new ArenaMeldingHeaders(new ArenaId("TA123123"), "tiltakskode", mockBruker.oppfolgingsperiodeId, null)));
        cronService.sendOppTil5000AktiviterTilPortefolje();
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_sende_nye_lonnstilskudd_til_portefolje() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        Aktivitetskort aktivitetskort = AktivitetskortUtil.ny(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT, ZonedDateTime.now(), mockBruker);
        KafkaAktivitetskortWrapperDTO wrapper = new KafkaAktivitetskortWrapperDTO(aktivitetskort, UUID.randomUUID(), MIDLERTIDIG_LONNSTILSKUDD, MessageSource.TEAM_TILTAK);
        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(wrapper));
        cronService.sendOppTil5000AktiviterTilPortefolje();
        ConsumerRecord<String, String> portefojeRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        KafkaAktivitetMeldingV4 melding = JsonUtils.fromJson(portefojeRecord.value(), KafkaAktivitetMeldingV4.class);

        Assertions.assertThat(melding.getTiltakskode()).isEqualTo(AktivitetTypeDTO.aktivitetsKortTypeToArenaTiltakskode(MIDLERTIDIG_LONNSTILSKUDD));
        Assertions.assertThat(melding.getAktivitetType()).isEqualTo(AktivitetTypeDTO.TILTAK);
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_ikke_sende_tiltak_opprettet_som_historisk() {
        MockBruker mockBruker = navMockService.createHappyBruker();
        // Happy bruker har en gammel periode startDato nå-100 dager, sluttDato nå-50 dager
        UUID funksjonellId = UUID.randomUUID();
        Aktivitetskort aktivitetskort = AktivitetskortUtil.ny(funksjonellId, AktivitetskortStatus.PLANLAGT, ZonedDateTime.now().minusDays(75), mockBruker);
        KafkaAktivitetskortWrapperDTO wrapper = new KafkaAktivitetskortWrapperDTO(aktivitetskort, funksjonellId, AktivitetskortType.ARENA_TILTAK, MessageSource.ARENA_TILTAK_AKTIVITET_ACL);
        ArenaId arenaId = new ArenaId("ARENATA123");
        String tiltakskode = "MIDLONNTIL";
        aktivitetTestService.opprettEksterntArenaKort(new ArenaKort(wrapper, new ArenaMeldingHeaders(arenaId, tiltakskode, mockBruker.oppfolgingsperiodeId, null)));
        cronService.sendOppTil5000AktiviterTilPortefolje();
        // Ingen nye meldinger på porteføljetopic
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(portefoljeTopic, portefoljeConsumer));
    }

    @Test
    void skal_committe_hver_melding() {
        MockBruker mockBruker = navMockService.createBruker(BrukerOptions.happyBruker());
        AktivitetData aktivitetData1 = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetData aktivitetData2 = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalSendes1 = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData1, false);
        AktivitetDTO skalSendes2 = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData2, false);

        aktivitetTestService.opprettAktivitet(mockBruker, navMockService.createVeileder(mockBruker), skalSendes1);
        aktivitetTestService.opprettAktivitet(mockBruker, navMockService.createVeileder(mockBruker), skalSendes2);

        Mockito
                .doCallRealMethod()
                .doThrow(IllegalStateException.class)
                .when(portefoljeProducer)
                .send((ProducerRecord<String, String>) Mockito.any(ProducerRecord.class));

//        assertThrows(IllegalStateException.class, () -> cronService.sendOppTil5000AktiviterTilPortefolje());
        cronService.sendOppTil5000AktiviterTilPortefolje();

        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AKTIVITET WHERE AKTIVITET.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NOT NULL", Long.class)).isEqualTo(1);
        Assertions.assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AKTIVITET WHERE AKTIVITET.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL", Long.class)).isEqualTo(1);

    }
}
