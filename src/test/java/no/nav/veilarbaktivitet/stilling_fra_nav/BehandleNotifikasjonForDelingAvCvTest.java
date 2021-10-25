package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
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

import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class BehandleNotifikasjonForDelingAvCvTest {

    BehandleNotifikasjonForDelingAvCvService behandleNotifikasjonForDelingAvCvService;


    @Autowired
    Credentials credentials;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Autowired
    SendOppgaveCron sendOppgaveCron;

    @Autowired
    KafkaTestService kafkaTestService;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveTopic;

    Consumer<Nokkel, Oppgave> oppgaveConsumer;

    @LocalServerPort
    private int port;

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
        oppgaveConsumer = kafkaTestService.createAvroAvroConsumer(oppgaveTopic);
    }

    @Test
    public void skalSendeHarVarsletForFerdigstiltNotifikasjon() {

        // sett opp testdata
        MockBruker mockBruker = MockNavService.crateHappyBruker();

        // Opprett stilling fra nav
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker, port);

        // trigger utsendelse av oppgave-notifikasjoner
        sendOppgaveCron.sendBrukernotifikasjoner();

        // simuler kvittering fra brukernotifikasjoner


        // les oppgave-notifikasjon
        final ConsumerRecord<Nokkel, Oppgave> consumerRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        String eventId = consumerRecord.key().getEventId();
        String brukernotifikasjonId = "O-" + credentials.username + "-" + eventId;

        DoknotifikasjonStatus doknotifikasjonStatus = doknotifikasjonStatus(eventId, EksternVarslingKvitteringConsumer.FERDIGSTILT);
        ConsumeStatus consumeStatus = eksternVarslingKvitteringConsumer.consume(new ConsumerRecord<>("kake", 1, 1, brukernotifikasjonId, doknotifikasjonStatus));


        int behandlede = behandleNotifikasjonForDelingAvCvService.behandleFerdigstilteNotifikasjoner();

        // sjekk at vi har sendt melding til rekrutteringsbistand

        // sjekk at StillingFraNav.LivslopStatus = HAR_VARSLET

        // sjekk at vi ikke behandler ting vi ikke skal behandle

        Assertions.assertThat(behandleNotifikasjonForDelingAvCvService.behandleFerdigstilteNotifikasjoner()).isEqualTo(0);

    }

    private DoknotifikasjonStatus doknotifikasjonStatus(String bestillingsId, String status) {
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId(credentials.username)
                .setMelding("her er en melding")
                .setDistribusjonId(null)
                .build();
    }
}
