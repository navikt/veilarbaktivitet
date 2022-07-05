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
    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
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
    private AktivitetDTO stillingFraNav;

    @Before
    public void cleanupBetweenTests() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljetopic);
    }

    @Test
    public void harIkkeSvart() {
        stillingFraNav = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        KafkaAktivitetMeldingV4 kafkaAktivitetMeldingV4 = lesSisteMelding(stillingFraNav.getId());

        Assertions.assertThat(kafkaAktivitetMeldingV4).isNotNull();
        Assertions.assertThat(kafkaAktivitetMeldingV4.getStillingFraNavData().cvKanDelesStatus()).isEqualTo(CvKanDelesStatus.IKKE_SVART);
    }

    @Test
    public void harSvartNei() {
        stillingFraNav = aktivitetTestService.opprettStillingFraNav(mockBruker);
        svarPaDelingAvCv(Boolean.FALSE);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        KafkaAktivitetMeldingV4 sisteMelding = lesSisteMelding(stillingFraNav.getId());
        Assertions.assertThat(sisteMelding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.NEI);
    }

    @Test
    public void harSvartJa() {
        stillingFraNav = aktivitetTestService.opprettStillingFraNav(mockBruker);
        svarPaDelingAvCv(Boolean.TRUE);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        KafkaAktivitetMeldingV4 sisteMelding = lesSisteMelding(stillingFraNav.getId());
        Assertions.assertThat(sisteMelding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.JA);
    }

    @Test
    public void annenAktivitet() {
        AktivitetData annenAktivitet = AktivitetDataTestBuilder.nyEgenaktivitet().withId(1337L);
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(annenAktivitet, false);
        aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        ConsumerRecord<String, String> jason = getSingleRecord(portefoljeConsumer, portefoljetopic, 10000);
        var kafkaAktivitetMeldingV4 = JsonUtils.fromJson(jason.value(), KafkaAktivitetMeldingV4.class);
        Assertions.assertThat(kafkaAktivitetMeldingV4.getStillingFraNavData()).isNull();
    }

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
        portefoljeConsumer.unsubscribe();
        portefoljeConsumer.close();
    }

    private KafkaAktivitetMeldingV4 lesSisteMelding(String id) {
        List<AktivitetDTO> aktivitetDTOS = aktivitetTestService.hentVersjoner(id, mockBruker, mockVeileder);
        KafkaAktivitetMeldingV4 sisteConsumerRecord = null;

        for (ConsumerRecord<String, String> r : getRecords(portefoljeConsumer, 10000, aktivitetDTOS.size())) {
            var melding = JsonUtils.fromJson(r.value(), KafkaAktivitetMeldingV4.class);
            if (melding.getAktivitetId().equals(id)) sisteConsumerRecord = melding;
        }

        return sisteConsumerRecord;
    }

    private void svarPaDelingAvCv(Boolean kanDeleCv) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1988);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        aktivitetTestService.svarPaaDelingAvCv(kanDeleCv, mockBruker, mockVeileder, stillingFraNav, cal.getTime());
    }
}
