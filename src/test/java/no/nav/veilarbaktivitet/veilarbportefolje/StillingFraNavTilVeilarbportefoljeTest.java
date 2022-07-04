package no.nav.veilarbaktivitet.veilarbportefolje;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@Slf4j
public class StillingFraNavTilVeilarbportefoljeTest extends SpringBootTestBase {
    @Value("${topic.ut.portefolje}")
    private String portefoljetopic;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> producer;

    Consumer<String, String> portefoljeConsumer;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    @Autowired
    AktiviteterTilKafkaService aktiviteterTilKafkaService;

    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        portefoljeConsumer.unsubscribe();
        portefoljeConsumer.close();
    }

    @Before
    public void cleanupBetweenTests() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljetopic);
    }

    @Test
    public void harIkkeSvart() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> jason = getSingleRecord(portefoljeConsumer, portefoljetopic, 10000);
        KafkaAktivitetMeldingV4 kafkaAktivitetMeldingV4 = JsonUtils.fromJson(jason.value(), KafkaAktivitetMeldingV4.class);

        Assertions.assertThat(kafkaAktivitetMeldingV4).isNotNull();
        Assertions.assertThat(kafkaAktivitetMeldingV4.getStillingFraNavData().cvKanDelesStatus()).isEqualTo(CvKanDelesStatus.IKKE_SVART);
    }

    public static final Date AVTALT_DATO;

    static {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1988);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        AVTALT_DATO = cal.getTime();
    }

    @Test
    public void harSvartNei() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.svarPaaDelingAvCv(Boolean.FALSE, mockBruker, mockVeileder, aktivitetDTO, AVTALT_DATO);

        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(aktivitetDTO.getId(), mockBruker, mockVeileder);

        ConsumerRecord<String, String> sisteConsumerRecord = null;

        for (ConsumerRecord<String, String> r : getRecords(portefoljeConsumer, 10000, aktivitetDTOS.size())) {
            sisteConsumerRecord = r;
        }

        Assertions.assertThat(sisteConsumerRecord).isNotNull();
        KafkaAktivitetMeldingV4 sisteMelding = JsonUtils.fromJson(sisteConsumerRecord.value(), KafkaAktivitetMeldingV4.class);
        Assertions.assertThat(sisteMelding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.NEI);
    }

    @Test
    public void harSvartJa() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktivitetTestService.svarPaaDelingAvCv(Boolean.TRUE, mockBruker, mockVeileder, aktivitetDTO, AVTALT_DATO);

        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(aktivitetDTO.getId(), mockBruker, mockVeileder);

        ConsumerRecord<String, String> sisteConsumerRecord = null;

        for (ConsumerRecord<String, String> r : getRecords(portefoljeConsumer, 10000, aktivitetDTOS.size())) {
            sisteConsumerRecord = r;
        }

        Assertions.assertThat(sisteConsumerRecord).isNotNull();
        KafkaAktivitetMeldingV4 sisteMelding = JsonUtils.fromJson(sisteConsumerRecord.value(), KafkaAktivitetMeldingV4.class);
        Assertions.assertThat(sisteMelding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.JA);
    }

    @Test
    public void annenAktivitet() {
        MockBruker happyBruker = MockNavService.createHappyBruker();

        AktivitetData nyMoteAktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(nyMoteAktivitet, false);
        aktivitetTestService.opprettAktivitet(happyBruker, aktivitetDTO);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> jason = getSingleRecord(portefoljeConsumer, portefoljetopic, 10000);

        KafkaAktivitetMeldingV4 kafkaAktivitetMeldingV4 = JsonUtils.fromJson(jason.value(), KafkaAktivitetMeldingV4.class);

        Assertions.assertThat(kafkaAktivitetMeldingV4).isNotNull();
        Assertions.assertThat(kafkaAktivitetMeldingV4.getStillingFraNavData()).isNull();
    }


}
