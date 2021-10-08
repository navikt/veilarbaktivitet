package no.nav.veilarbaktivitet.brukernotifikasjon;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKviteringConsumer;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class BrukernotifikasjonKvittering {

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    AktivitetTestService aktivitetTestService;

    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendOppgaveCron sendOppgaveCron;

    @Autowired
    KafkaTestService kafkaTestService;

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    String oppgaveTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    String doneTopic;

    Consumer<Nokkel, Done> doneConsumer;

    Consumer<Nokkel, Oppgave> oppgaveConsumer;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    Credentials credentials;

    @Value("${topic.inn.ekstertVarselKvitering}")
    String kviteringsToppic;

    @Autowired
    KafkaTemplate<String, DoknotifikasjonStatus> kviteringsTopic;

    @Autowired
    EksternVarslingKviteringConsumer eksternVarslingKviteringConsumer;

    @LocalServerPort
    private int port;

    @Value("${app.env.aktivitetsplan.basepath}")
    String basepath;

    @Before
    public void setUp() {
        DbTestUtils.cleanupTestDb(jdbc.getJdbcTemplate());

        oppgaveConsumer = kafkaTestService.createAvroAvroConsumer(oppgaveTopic);
        doneConsumer = kafkaTestService.createAvroAvroConsumer(doneTopic);
    }

    @After
    public void assertNoUnkowns() {
        oppgaveConsumer.unsubscribe();
        doneConsumer.unsubscribe();

        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    public void happy_case() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = oppretOppgave(mockBruker, aktivitetDTO);

        String eventId = oppgaveRecord.key().getEventId();
        String brukernotifikasjonId = "O-" + credentials.username + "-" + eventId;

        assertVarselStatus(eventId, VarselStatus.FORSOKT_SENDT);

        DoknotifikasjonStatus status = okStatus(eventId);
        status.setBestillerId("annen_bestillerid");
        assertStatus(eventId, brukernotifikasjonId, status, ConsumeStatus.OK, VarselStatus.FORSOKT_SENDT);

        assertStatus(eventId, brukernotifikasjonId, okStatus(brukernotifikasjonId), ConsumeStatus.OK, VarselStatus.SENDT_OK);

        assertStatus(eventId, brukernotifikasjonId, infoStatus(brukernotifikasjonId), ConsumeStatus.OK, VarselStatus.SENDT_OK);

        assertStatus(eventId, brukernotifikasjonId, oversendtStatus(brukernotifikasjonId), ConsumeStatus.OK, VarselStatus.SENDT_OK);

        assertStatus(eventId, brukernotifikasjonId, feiletStatus(brukernotifikasjonId), ConsumeStatus.OK, VarselStatus.FEILET);

        assertStatus(eventId, brukernotifikasjonId, status(brukernotifikasjonId, "ugyldig_status"), ConsumeStatus.FAILED, VarselStatus.FEILET);
    }

    private void assertStatus(String eventId, String brukernotifikasjonId, DoknotifikasjonStatus message, ConsumeStatus expectedConsumeStatus, VarselStatus expectedVarselStatus) {
        ConsumeStatus consumeStatus = eksternVarslingKviteringConsumer.consume(new ConsumerRecord<>("kake", 1, 1, brukernotifikasjonId, message));
        assertEquals(expectedConsumeStatus, consumeStatus);

        assertVarselStatus(eventId, expectedVarselStatus);
    }

    private void assertVarselStatus(String eventId, VarselStatus expectedVarselStatus) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String status = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(expectedVarselStatus.name(), status);
    }

    private DoknotifikasjonStatus status(String bestillingsId, String status) {
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus(status)
                .setBestillingsId(bestillingsId)
                .setBestillerId(credentials.username)
                .setMelding("her er en melding")
                .setDistribusjonId(null)
                .build();
    }

    private DoknotifikasjonStatus okStatus(String bestillingsId) {
        return status(bestillingsId, "FERDISTSTILT");
    }

    private DoknotifikasjonStatus feiletStatus(String bestillingsId) {
        return status(bestillingsId, "FEILET");
    }

    private DoknotifikasjonStatus infoStatus(String bestillingsId) {
        return status(bestillingsId, "INFO");
    }

    private DoknotifikasjonStatus oversendtStatus(String bestillingsId) {
        return status(bestillingsId, "OVERSENDT");
    }

    private ConsumerRecord<Nokkel, Oppgave> oppretOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettOppgavePaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV
        );

        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        Oppgave oppgave = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsPeriode().toString(), oppgave.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), oppgave.getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgaveRecord;
    }
}
