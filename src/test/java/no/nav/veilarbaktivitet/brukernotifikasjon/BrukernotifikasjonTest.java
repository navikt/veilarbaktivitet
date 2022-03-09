package no.nav.veilarbaktivitet.brukernotifikasjon;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.EksternVarslingKvitteringConsumer;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
public class BrukernotifikasjonTest {

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

    @Value("${topic.ut.brukernotifikasjon.beskjed}")
    String beskjedTopic;

    @Value("${topic.ut.brukernotifikasjon.done}")
    String doneTopic;

    Consumer<NokkelInput, DoneInput> doneConsumer;

    Consumer<NokkelInput, OppgaveInput> oppgaveConsumer;

    Consumer<Nokkel, Beskjed> beskjedConsumer;

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
        beskjedConsumer = kafkaTestService.createAvroAvroConsumer(beskjedTopic);
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
    public void happy_case_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        oppgaveSendtOk(oppgaveRecord);
        avsluttOppgave(mockBruker, aktivitetDTO, oppgaveRecord);
    }

    @SneakyThrows
    @Test
    public void skalSendeOppgaveMedEgentTekst() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        String epostTitel = "epostTitel";
        String epostTekst = "EpostTekst";
        String SMSTekst = "SMSTekst";

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV,
                epostTitel,
                epostTekst,
                SMSTekst
        );

        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 5000);
        Oppgave oppgave = oppgaveRecord.value();

        assertEquals(epostTitel, oppgave.getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getSmsVarslingstekst());
        oppgaveSendtOk(oppgaveRecord);
        avsluttOppgave(mockBruker, aktivitetDTO, oppgaveRecord);
    }

    @SneakyThrows
    @Test
    public void skalSendeBeskjedMedEgentTekst() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        String epostTitel = "epostTitel";
        String epostTekst = "EpostTekst";
        String SMSTekst = "SMSTekst";

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.MOTE_SMS,
                epostTitel,
                epostTekst,
                SMSTekst
        );

        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        final ConsumerRecord<Nokkel, Beskjed> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        Beskjed oppgave = oppgaveRecord.value();

        assertEquals(epostTitel, oppgave.getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getSmsVarslingstekst());
    }

    @SneakyThrows
    @Test
    public void skal_sendeBeskjed() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.MOTE_SMS
        );

        sendOppgaveCron.sendBrukernotifikasjoner();

        final ConsumerRecord<Nokkel, Beskjed> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 5000);
        Beskjed oppgave = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), oppgave.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), oppgave.getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
    }

    @Test
    public void skal_ikke_produsere_meldinger_for_avsluttet_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(Long.parseLong(aktivitetDTO.getId()), Long.parseLong(aktivitetDTO.getVersjon()), Person.aktorId(mockBruker.getAktorId()), "Testvarsel", VarselType.STILLING_FRA_NAV);
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);

        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
    }

    @Test
    public void skal_ikke_sende_meldinger_for_avbrutte_aktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);
        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV
        );

        Response response = mockBruker
                .createRequest()
                .and()
                .body(aktivitetDTO.toBuilder().status(AktivitetStatus.AVBRUTT).avsluttetKommentar("Kake").build())
                .when()
                .put(mockBruker.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/status", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO avbruttAktivitet = response.as(AktivitetDTO.class);

        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(port, mockBruker);
        AktivitetDTO skalIkkeVaereOppdatert = AktivitetTestService.finnAktivitet(aktivitetsplanDTO, avbruttAktivitet.getId());

        assertEquals(avbruttAktivitet, skalIkkeVaereOppdatert);
    }

    @Test
    public void skal_avslutte_avbrutteAktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(port, mockBruker, skalOpprettes);

        final ConsumerRecord<Nokkel, Oppgave> oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        oppgaveSendtOk(oppgaveRecord);

        mockBruker
                .createRequest()
                .and()
                .body(aktivitetDTO.toBuilder().status(AktivitetStatus.AVBRUTT).avsluttetKommentar("Kake").build())
                .when()
                .put(mockBruker.getUrl("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/status", mockBruker))
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();


        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));

        final ConsumerRecord<Nokkel, Done> doneRecord = getSingleRecord(doneConsumer, doneTopic, 5000);
        Done done = doneRecord.value();

        assertEquals(oppgaveRecord.key().getSystembruker(), doneRecord.key().getSystembruker());
        assertEquals(oppgaveRecord.key().getEventId(), doneRecord.key().getEventId());

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), done.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), done.getFodselsnummer());
    }


    private void oppgaveSendtOk(ConsumerRecord<Nokkel, Oppgave> oppgaveRecord) {
        String eventId = oppgaveRecord.key().getEventId();
        String brukernotifikasjonId = "O-" + credentials.username + "-" + eventId;

        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String forsoktSendt = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(VarselStatus.SENDT.name(), forsoktSendt);
    }

    private DoknotifikasjonStatus okStatus(String bestillingsId) {
        return DoknotifikasjonStatus
                .newBuilder()
                .setStatus("FERDIGSTILT")
                .setBestillingsId(bestillingsId)
                .setBestillerId(credentials.username)
                .setMelding("her er en melling")
                .setDistribusjonId(null)
                .build();
    }

    private ConsumerRecord<Nokkel, Oppgave> opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettVarselPaaAktivitet(
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

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), oppgave.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), oppgave.getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgaveRecord;
    }

    private void avsluttOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO, ConsumerRecord<Nokkel, Oppgave> oppgaveRecord) {
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);
        sendOppgaveCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsere oppgave", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        final ConsumerRecord<Nokkel, Done> doneRecord = getSingleRecord(doneConsumer, doneTopic, 5000);
        Done done = doneRecord.value();

        assertEquals(oppgaveRecord.key().getSystembruker(), doneRecord.key().getSystembruker());
        assertEquals(oppgaveRecord.key().getEventId(), doneRecord.key().getEventId());

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), done.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), done.getFodselsnummer());
    }

}