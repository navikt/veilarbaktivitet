package no.nav.veilarbaktivitet.veilarbportefolje;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4;
import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Calendar;

import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

public class StillingFraNavTilVeilarbportefoljeTest extends SpringBootTestBase {
    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
    @Value("${topic.ut.portefolje}")
    private String portefoljetopic;

    @Value("${spring.kafka.consumer.group-id}")
    String groupId;

    @Autowired
    KafkaStringAvroTemplate<ForesporselOmDelingAvCv> producer;

    Consumer<String, String> portefoljeConsumer;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    @Autowired
    AktiviteterTilKafkaService aktiviteterTilKafkaService;

    @BeforeEach
    public void startConsumer() {
        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljetopic);
    }

    @AfterEach
    public void cleanlyCloseConsumer() {
        portefoljeConsumer.unsubscribe();
        portefoljeConsumer.close();
    }

    @Test
    public void harIkkeSvart() {
        aktivitetTestService.opprettStillingFraNav(mockBruker);
        clearKafkaTopic();
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();

        KafkaAktivitetMeldingV4 melding = getMelding();

        Assertions.assertThat(melding).isNotNull();
        Assertions.assertThat(melding.getStillingFraNavData().cvKanDelesStatus()).isEqualTo(CvKanDelesStatus.IKKE_SVART);
    }

    @Test
    public void harSvartNei() {
        var stillingFraNav = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        svarPaDelingAvCv(Boolean.FALSE, stillingFraNav);

        clearKafkaTopic();
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        var melding = getMelding();

        Assertions.assertThat(melding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.NEI);
    }

    @Test
    public void harSvartJa() {
        var stillingFraNav = aktivitetTestService.opprettStillingFraNav(mockBruker);
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        svarPaDelingAvCv(Boolean.TRUE, stillingFraNav);

        clearKafkaTopic();
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        var melding = getMelding();

        Assertions.assertThat(melding.getStillingFraNavData().cvKanDelesStatus()).isSameAs(CvKanDelesStatus.JA);
    }

    @Test
    public void annenAktivitet() {
        AktivitetData annenAktivitet = AktivitetDataTestBuilder.nyEgenaktivitet().withId(1337L);
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(annenAktivitet, false);
        aktivitetTestService.opprettAktivitet(mockBruker, aktivitetDTO);

        clearKafkaTopic();
        aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        var melding = getMelding();

        Assertions.assertThat(melding.getStillingFraNavData()).isNull();
    }

    private KafkaAktivitetMeldingV4 getMelding() {
        return JsonUtils.fromJson(getSingleRecord(portefoljeConsumer, portefoljetopic, 10000).value(), KafkaAktivitetMeldingV4.class);
    }

    private void clearKafkaTopic() {
        portefoljeConsumer.unsubscribe();
        portefoljeConsumer.close();
        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljetopic);
    }

    private void svarPaDelingAvCv(Boolean kanDeleCv, AktivitetDTO stillingFraNav) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1988);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        aktivitetTestService.svarPaaDelingAvCv(kanDeleCv, mockBruker, mockVeileder, stillingFraNav, cal.getTime());
    }
}
