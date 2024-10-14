package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class BehandleNotifikasjonForDelingAvCvTest extends SpringBootTestBase {

    @Autowired
    BehandleNotifikasjonForDelingAvCvCronService behandleNotifikasjonForDelingAvCvCronService;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    Consumer<String, DelingAvCvRespons> rekrutteringsbistandConsumer;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @BeforeEach
    public void cleanupBetweenTests() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        DbTestUtils.cleanupTestDb(jdbc);

    }

    @Test //TODO rydd denne testen
    void skalSendeHarVarsletForFerdigstiltNotifikasjonIkkeSvart() {

        // sett opp testdata
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        // Opprett stilling fra nav og send varsel
        AktivitetDTO utenSvar = aktivitetTestService.opprettStillingFraNav(mockBruker);
        var utenSvarOppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        AktivitetDTO skalFaaSvar = aktivitetTestService.opprettStillingFraNav(mockBruker);
        var medSvarOppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        AktivitetDTO medSvar = aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, skalFaaSvar, new Date());

        //kviteringer på varsel
        brukernotifikasjonAsserts.simulerEksternVarselSendt(utenSvarOppgave);
        brukernotifikasjonAsserts.simulerEksternVarselSendt(medSvarOppgave);


        //Send meldinger til rekruteringsbistand
        rekrutteringsbistandConsumer = kafkaTestService.createStringAvroConsumer(utTopic);
        int behandlede = behandleNotifikasjonForDelingAvCvCronService.behandleFerdigstilteNotifikasjoner(500);
        assertThat(behandlede).isEqualTo(2);

        // sjekk at vi har sendt melding til rekrutteringsbistand
        ConsumerRecord<String, DelingAvCvRespons> delingAvCvResponsRecord = getSingleRecord(rekrutteringsbistandConsumer, utTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        assertThat(delingAvCvResponsRecord.value().getBestillingsId()).isEqualTo(utenSvar.getStillingFraNavData().getBestillingsId());
        assertThat(delingAvCvResponsRecord.value().getTilstand()).isEqualTo(TilstandEnum.HAR_VARSLET);

        assertThat(kafkaTestService.harKonsumertAlleMeldinger(utTopic, rekrutteringsbistandConsumer)).isTrue();

        // sjekk at StillingFraNav.LivslopStatus = HAR_VARSLET
        AktivitetDTO behandletAktivitet = aktivitetTestService.hentAktivitet(mockBruker, veileder, utenSvar.getId());
        AktivitetDTO expectedAktivitet = behandletAktivitet.toBuilder().stillingFraNavData(behandletAktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.HAR_VARSLET)).build();
        AktivitetAssertUtils.assertOppdatertAktivitet(expectedAktivitet, behandletAktivitet);

        AktivitetDTO ikkeBehandletAktivitet = aktivitetTestService.hentAktivitet(mockBruker, veileder, medSvar.getId());
        AktivitetAssertUtils.assertOppdatertAktivitet(medSvar, ikkeBehandletAktivitet);

        // sjekk at vi ikke behandler ting vi ikke skal behandle
        assertThat(behandleNotifikasjonForDelingAvCvCronService.behandleFerdigstilteNotifikasjoner(500)).isZero();
    }

    @Test
        //TODO rydd denne testen
    void skalSendeKanIkkeVarsleForFeiledeNotifikasjonIkkeSvart() {
        // sett opp testdata
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(mockBruker);

        // Opprett stilling fra nav
        AktivitetDTO utenSvar = aktivitetTestService.opprettStillingFraNav(mockBruker);
        var utenSvarOppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        AktivitetDTO skalFaaSvar = aktivitetTestService.opprettStillingFraNav(mockBruker);
        var medSvarOppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());

        // trigger utsendelse av oppgave-notifikasjoner
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();

        AktivitetDTO medSvar = aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, skalFaaSvar, new Date());

        // simuler kvittering fra brukernotifikasjoner

        // les oppgave-notifikasjon
        brukernotifikasjonAsserts.simulerEksternVarselFeilet(utenSvarOppgave);
        brukernotifikasjonAsserts.simulerEksternVarselFeilet(medSvarOppgave);

        rekrutteringsbistandConsumer = kafkaTestService.createStringAvroConsumer(utTopic);
        int behandlede = behandleNotifikasjonForDelingAvCvCronService.behandleFeiledeNotifikasjoner(500);
        assertThat(behandlede).isEqualTo(2);

        // sjekk at vi har sendt melding til rekrutteringsbistand
        ConsumerRecord<String, DelingAvCvRespons> delingAvCvResponsRecord = getSingleRecord(rekrutteringsbistandConsumer, utTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        assertThat(delingAvCvResponsRecord.value().getBestillingsId()).isEqualTo(utenSvar.getStillingFraNavData().getBestillingsId());
        assertThat(delingAvCvResponsRecord.value().getTilstand()).isEqualTo(TilstandEnum.KAN_IKKE_VARSLE);

        assertThat(kafkaTestService.harKonsumertAlleMeldinger(utTopic, rekrutteringsbistandConsumer)).isTrue();

        // sjekk at StillingFraNav.LivslopStatus = KAN_IKKE_VARSLE
        AktivitetDTO behandletAktivitet = aktivitetTestService.hentAktivitet(mockBruker, veileder, utenSvar.getId());
        AktivitetDTO expectedAktivitet = behandletAktivitet.toBuilder().stillingFraNavData(behandletAktivitet.getStillingFraNavData().withLivslopsStatus(LivslopsStatus.KAN_IKKE_VARSLE)).build();
        AktivitetAssertUtils.assertOppdatertAktivitet(expectedAktivitet, behandletAktivitet);

        AktivitetDTO ikkeBehandletAktivitet = aktivitetTestService.hentAktivitet(mockBruker, veileder, medSvar.getId());
        AktivitetAssertUtils.assertOppdatertAktivitet(medSvar, ikkeBehandletAktivitet);

        // sjekk at vi ikke behandler ting vi ikke skal behandle
        assertThat(behandleNotifikasjonForDelingAvCvCronService.behandleFerdigstilteNotifikasjoner(500)).isZero();
    }
}
