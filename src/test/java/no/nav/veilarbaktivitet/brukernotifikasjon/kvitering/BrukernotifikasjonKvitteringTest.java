package no.nav.veilarbaktivitet.brukernotifikasjon.kvitering;

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
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.oppgave.SendOppgaveCron;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroTemplate;
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
import org.springframework.test.context.junit4.SpringRunner;

import static no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class BrukernotifikasjonKvitteringTest {

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

    @Value("${topic.inn.eksternVarselKvittering}")
    String kviteringsToppic;

    @Autowired
    KafkaAvroTemplate<DoknotifikasjonStatus> kviteringsTopic;

    @Autowired
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

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
    public void status_tester() {
        MockBruker mockBruker = MockNavService.crateHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        String eventId = oppgaveRecord.key().getEventId();

        assertVarselStatusErSendt(eventId);
        assertEksternVarselStatus(eventId, VarselKviteringStatus.IKKE_SATT);

        skalIkkeBehandleMedAnnenBestillingsId(eventId);

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKviteringStatus.IKKE_SATT);

        consumAndAssertStatus(eventId, okStatus(eventId), VarselKviteringStatus.OK);

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKviteringStatus.OK);

        consumAndAssertStatus(eventId, feiletStatus(eventId), VarselKviteringStatus.FEILET);
        consumAndAssertStatus(eventId, okStatus(eventId), VarselKviteringStatus.FEILET);

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKviteringStatus.FEILET);

        consumAndAssertStatus(eventId, status(eventId, "ugyldig_status"), ConsumeStatus.FAILED, VarselKviteringStatus.FEILET);
    }

    private void infoOgOVersendtSkalIkkeEndreStatus(String eventId, VarselKviteringStatus expectedVarselKviteringStatus) {
        consumAndAssertStatus(eventId, infoStatus(eventId), expectedVarselKviteringStatus);
        consumAndAssertStatus(eventId, oversendtStatus(eventId), expectedVarselKviteringStatus);
    }

    private void skalIkkeBehandleMedAnnenBestillingsId(String eventId) {
        DoknotifikasjonStatus statusMedAnnenBestillerId = okStatus(eventId);
        statusMedAnnenBestillerId.setBestillerId("annen_bestillerid");

        consumAndAssertStatus(eventId, statusMedAnnenBestillerId, VarselKviteringStatus.IKKE_SATT);
    }


    private void consumAndAssertStatus(String eventId, DoknotifikasjonStatus message, VarselKviteringStatus expectedEksternVarselStatus) {
        consumAndAssertStatus(eventId, message, ConsumeStatus.OK, expectedEksternVarselStatus);
    }

    private void consumAndAssertStatus(String eventId, DoknotifikasjonStatus message, ConsumeStatus expectedConsumeStatus, VarselKviteringStatus expectedEksternVarselStatus) {
        String brukernotifikasjonId = "O-" + credentials.username + "-" + eventId;
        ConsumeStatus consumeStatus = eksternVarslingKvitteringConsumer.consume(new ConsumerRecord<>("VarselKviteringToppic", 1, 1, brukernotifikasjonId, message));
        assertEquals(expectedConsumeStatus, consumeStatus);

        assertVarselStatusErSendt(eventId);
        assertEksternVarselStatus(eventId, expectedEksternVarselStatus);
    }

    private void assertVarselStatusErSendt(String eventId) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String status = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(VarselStatus.SENDT.name(), status);
    }

    private void assertEksternVarselStatus(String eventId, VarselKviteringStatus expectedVarselStatus) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String status = jdbc.queryForObject("SELECT EKSTERNT_VARSEL_STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(expectedVarselStatus.name(), status);
    }

    private DoknotifikasjonStatus status(String eventId, String status) {
        String bestillingsId = "O-" + credentials.username + "-" + eventId;
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
        return status(bestillingsId, FERDIGSTILT);
    }

    private DoknotifikasjonStatus feiletStatus(String bestillingsId) {
        return status(bestillingsId, FEILET);
    }

    private DoknotifikasjonStatus infoStatus(String bestillingsId) {
        return status(bestillingsId, INFO);
    }

    private DoknotifikasjonStatus oversendtStatus(String eventId) {
        return status(eventId, OVERSENDT);
    }

    private ConsumerRecord<Nokkel, Oppgave> opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
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
