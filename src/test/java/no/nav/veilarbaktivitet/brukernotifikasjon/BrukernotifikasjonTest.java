package no.nav.veilarbaktivitet.brukernotifikasjon;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

public class BrukernotifikasjonTest extends SpringBootTestBase {

    @Value("${app.env.appname}")
    private String appname;
    @Value("${app.env.namespace}")
    private String namespace;

    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

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

    Consumer<NokkelInput, BeskjedInput> beskjedConsumer;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    Credentials credentials;

    @Value("${topic.inn.eksternVarselKvittering}")
    String kviteringsToppic;

    @Autowired
    KafkaStringAvroTemplate<DoknotifikasjonStatus> kviteringsTopic;

    @Autowired
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @LocalServerPort
    private int port;

    @Value("${app.env.aktivitetsplan.basepath}")
    String basepath;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @Before
    public void setUp() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
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
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        oppgaveSendtOk(oppgaveRecord);
        avsluttOppgave(mockBruker, aktivitetDTO, oppgaveRecord);
    }

    @SneakyThrows
    @Test
    public void skalSendeOppgaveMedEgentTekst() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

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
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, OppgaveInput> oppgaveSendt = getSingleRecord(oppgaveConsumer, oppgaveTopic, 10000);

        NokkelInput key = oppgaveSendt.key();
        assertEquals(mockBruker.getFnr(), key.getFodselsnummer());
        assertEquals(appname, key.getAppnavn());
        assertEquals(namespace, key.getNamespace());
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        OppgaveInput oppgave = oppgaveSendt.value();

        assertEquals(epostTitel, oppgave.getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getSmsVarslingstekst());
        oppgaveSendtOk(oppgaveSendt);
        avsluttOppgave(mockBruker, aktivitetDTO, oppgaveSendt);
    }

    @SneakyThrows
    @Test
    public void skalSendeBeskjedMedEgentTekst() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

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

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        final ConsumerRecord<NokkelInput, BeskjedInput> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 10000);
        BeskjedInput oppgave = oppgaveRecord.value();

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
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.MOTE_SMS
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();

        final ConsumerRecord<NokkelInput, BeskjedInput> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, 10000);
        NokkelInput nokkel = oppgaveRecord.key();
        BeskjedInput beskjed = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), nokkel.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), nokkel.getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), beskjed.getLink());
    }

    @Test
    public void skal_ikke_produsere_meldinger_for_avsluttet_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(Long.parseLong(aktivitetDTO.getId()), Long.parseLong(aktivitetDTO.getVersjon()), Person.aktorId(mockBruker.getAktorId()), "Testvarsel", VarselType.STILLING_FRA_NAV);
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
    }

    @Test
    public void skal_ikke_sende_meldinger_for_avbrutte_aktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);
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

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO skalIkkeVaereOppdatert = AktivitetTestService.finnAktivitet(aktivitetsplanDTO, avbruttAktivitet.getId());

        assertEquals(avbruttAktivitet, skalIkkeVaereOppdatert);
    }

    @Test
    public void skal_avslutte_avbrutteAktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
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


        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert oppgave meldinger", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));

        final ConsumerRecord<NokkelInput, DoneInput> doneRecord = getSingleRecord(doneConsumer, doneTopic, 10000);

        NokkelInput oppgaveNokkel = oppgaveRecord.key();
        NokkelInput doneNokkel = doneRecord.key();

        assertEquals(oppgaveNokkel.getAppnavn(), doneNokkel.getAppnavn());
        assertEquals(oppgaveNokkel.getNamespace(), doneNokkel.getNamespace());
        assertEquals(oppgaveNokkel.getEventId(), doneNokkel.getEventId());

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), doneNokkel.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), doneNokkel.getFodselsnummer());
    }


    private void oppgaveSendtOk(ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord) {
        String eventId = oppgaveRecord.key().getEventId();

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

    private ConsumerRecord<NokkelInput, OppgaveInput> opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                Person.aktorId(mockBruker.getAktorId()),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsert done meldinger", kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer));
        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, 10000);
        OppgaveInput oppgave = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), oppgaveRecord.key().getGrupperingsId());
        assertEquals(mockBruker.getFnr(), oppgaveRecord.key().getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgaveRecord;
    }

    private void avsluttOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO, ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord) {
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue("Skal ikke produsere oppgave", kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer));
        final ConsumerRecord<NokkelInput, DoneInput> doneRecord = getSingleRecord(doneConsumer, doneTopic, 10000);
        DoneInput done = doneRecord.value();

        assertEquals(oppgaveRecord.key().getAppnavn(), doneRecord.key().getAppnavn());
        assertEquals(oppgaveRecord.key().getEventId(), doneRecord.key().getEventId());

        assertEquals(mockBruker.getOppfolgingsperiode().toString(), doneRecord.key().getGrupperingsId());
        assertEquals(mockBruker.getFnr(), doneRecord.key().getFodselsnummer());
    }

}