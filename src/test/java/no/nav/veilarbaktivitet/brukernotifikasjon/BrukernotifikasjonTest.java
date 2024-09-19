package no.nav.veilarbaktivitet.brukernotifikasjon;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitetskort.*;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.AktivitetTestService;
import no.nav.veilarbaktivitet.util.KafkaTestService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

class BrukernotifikasjonTest extends SpringBootTestBase {

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
    EksternVarslingKvitteringConsumer eksternVarslingKvitteringConsumer;

    @LocalServerPort
    private int port;

    @Value("${app.env.aktivitetsplan.basepath}")
    String basepath;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @BeforeEach
    void setUp() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        DbTestUtils.cleanupTestDb(jdbc.getJdbcTemplate());

        oppgaveConsumer = kafkaTestService.createAvroAvroConsumer(oppgaveTopic);
        beskjedConsumer = kafkaTestService.createAvroAvroConsumer(beskjedTopic);
        doneConsumer = kafkaTestService.createAvroAvroConsumer(doneTopic);
        when(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true);
    }

    @AfterEach
    void assertNoUnkowns() {
        oppgaveConsumer.unsubscribe();
        doneConsumer.unsubscribe();

        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    void happy_case_oppgave() {
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
    void skalSendeOppgaveMedEgentTekst() {
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
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV,
                epostTitel,
                epostTekst,
                SMSTekst
        );
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        ConsumerRecord<NokkelInput, OppgaveInput> oppgaveSendt = getSingleRecord(oppgaveConsumer, oppgaveTopic, DEFAULT_WAIT_TIMEOUT_DURATION);

        NokkelInput key = oppgaveSendt.key();
        assertEquals(mockBruker.getFnr(), key.getFodselsnummer());
        assertEquals(appname, key.getAppnavn());
        assertEquals(namespace, key.getNamespace());
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");
        OppgaveInput oppgave = oppgaveSendt.value();

        assertEquals(epostTitel, oppgave.getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getSmsVarslingstekst());
        oppgaveSendtOk(oppgaveSendt);
        avsluttOppgave(mockBruker, aktivitetDTO, oppgaveSendt);
    }

    @SneakyThrows
    @Test
    void skalSendeBeskjedMedEgentTekst() {
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
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.MOTE_SMS,
                epostTitel,
                epostTekst,
                SMSTekst
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");
        final ConsumerRecord<NokkelInput, BeskjedInput> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        BeskjedInput oppgave = oppgaveRecord.value();

        assertEquals(epostTitel, oppgave.getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getSmsVarslingstekst());
    }

    @SneakyThrows
    @Test
    void skal_sendeBeskjed() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.MOTE_SMS
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();

        final ConsumerRecord<NokkelInput, BeskjedInput> oppgaveRecord = getSingleRecord(beskjedConsumer, beskjedTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        NokkelInput nokkel = oppgaveRecord.key();
        BeskjedInput beskjed = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsperiodeId().toString(), nokkel.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), nokkel.getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), beskjed.getLink());
    }

    @Test
    void skal_ikke_produsere_meldinger_for_avsluttet_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(Long.parseLong(aktivitetDTO.getId()), Long.parseLong(aktivitetDTO.getVersjon()), mockBruker.getAktorId(), "Testvarsel", VarselType.STILLING_FRA_NAV);
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer), "Skal ikke produsert oppgave meldinger");
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");
    }

    @Test
    void skal_ikke_sende_meldinger_for_avbrutte_aktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);
        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV
        );

        Response response = mockBruker
                .createRequest()
                .and()
                .body(aktivitetDTO.toBuilder().status(AktivitetStatus.AVBRUTT).avsluttetKommentar("Kake").build())
                .when()
                .put("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/status")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();

        AktivitetDTO avbruttAktivitet = response.as(AktivitetDTO.class);

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer), "Skal ikke produsert oppgave meldinger");
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO skalIkkeVaereOppdatert = AktivitetTestService.finnAktivitet(aktivitetsplanDTO, avbruttAktivitet.getId());

        assertEquals(avbruttAktivitet, skalIkkeVaereOppdatert);
    }

    @Test
    void skal_lukke_brukernotifikasjonsOppgave_nar_aktivitet_blir_avbrutt() {
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
                .put("http://localhost:" + port + "/veilarbaktivitet/api/aktivitet/" + aktivitetDTO.getId() + "/status")
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract().response();


        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer), "Skal ikke produsert oppgave meldinger");

        final ConsumerRecord<NokkelInput, DoneInput> doneRecord = getSingleRecord(doneConsumer, doneTopic, DEFAULT_WAIT_TIMEOUT_DURATION);

        NokkelInput oppgaveNokkel = oppgaveRecord.key();
        NokkelInput doneNokkel = doneRecord.key();

        assertEquals(oppgaveNokkel.getAppnavn(), doneNokkel.getAppnavn());
        assertEquals(oppgaveNokkel.getNamespace(), doneNokkel.getNamespace());
        assertEquals(oppgaveNokkel.getEventId(), doneNokkel.getEventId());

        assertEquals(mockBruker.getOppfolgingsperiodeId().toString(), doneNokkel.getGrupperingsId());
        assertEquals(mockBruker.getFnr(), doneNokkel.getFodselsnummer());
    }


    private void oppgaveSendtOk(ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord) {
        String eventId = oppgaveRecord.key().getEventId();

        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", eventId);
        String forsoktSendt = jdbc.queryForObject("SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param, String.class);//TODO fiks denne når vi eksponerer det ut til apiet
        assertEquals(VarselStatus.SENDT.name(), forsoktSendt);
    }


    @Test
    void skal_kunne_opprette_brukernotifikasjon_pa_fho_pa_arena_aktiviteter_som_ikke_er_migrert_og_ha_lenke_med_riktig_id() {
        var mockBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        var mockVeileder = MockNavService.createVeileder(mockBruker);
        var arenaId = new ArenaId("ARENATA123");
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaId, mockVeileder);

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        var lenke = oppgaveRecord.value().getLink();
        assertEquals(lenke, String.format("http://localhost:3000/aktivitet/vis/%s", arenaId.id()));
    }

    @Test
    void skal_kunne_opprette_brukernotifications_pa_fho_pa_arena_aktiviteter_som_ER_migrert_og_ha_lenke_med_riktig_id() {
        when(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true);
        var mockBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        var mockVeileder = MockNavService.createVeileder(mockBruker);
        var arenaId = new ArenaId("ARENATA123");
        // Opprett ekstern aktivitet
        var aktivitetskortMelding = new KafkaAktivitetskortWrapperDTO(
                AktivitetskortUtil.ny(
                        UUID.randomUUID(),
                        AktivitetskortStatus.GJENNOMFORES,
                        ZonedDateTime.now(),
                        mockBruker
                ),
                AktivitetskortType.ARENA_TILTAK,
                MessageSource.ARENA_TILTAK_AKTIVITET_ACL
        );
        var headers = new ArenaMeldingHeaders(arenaId, "MIDL", mockBruker.oppfolgingsperiodeId, null);
        aktivitetTestService.opprettEksterntArenaKort(new ArenaKort(aktivitetskortMelding, headers));
        // Opprett fho når toggle er av
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaId, mockVeileder);
        // Assert url bruker teknisk id og ikke arenaId
        var funksjonellId = aktivitetskortMelding.getAktivitetskortId();
        var aktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, mockVeileder, funksjonellId);
        var tekniskId = aktivitet.getId();

        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        var lenke = oppgaveRecord.value().getLink();
        assertEquals(lenke, String.format("http://localhost:3000/aktivitet/vis/%s", tekniskId));
    }

    @Test
    void skal_lukke_brukernotifikasjonsOppgave_nar_eksterne_lonnstilskudd_blir_avbrutt() {
        var mockBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        var mockVeileder = MockNavService.createVeileder(mockBruker);
        // Opprett ekstern aktivitet
        var serie = new AktivitetskortSerie(mockBruker, AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);
        var aktivitetskortMelding = serie.ny(AktivitetskortStatus.GJENNOMFORES, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(aktivitetskortMelding));
        // Opprett FHO
        var aktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, mockVeileder, serie.getFunksjonellId());
        var avtaltMedNavDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(Long.parseLong(aktivitet.getVersjon()))
                .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(Type.SEND_FORHAANDSORIENTERING)
                        .tekst("lol").lestDato(null).build());
        aktivitetTestService.opprettFHOForInternAktivitet(mockBruker, mockVeileder, avtaltMedNavDTO, Long.parseLong(aktivitet.getId()));
        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        // Sett til avbrutt
        var avbruttAktivitet = serie.ny(AktivitetskortStatus.AVBRUTT, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(avbruttAktivitet));
        brukernotifikasjonAsserts.assertDone(oppgave.key());
    }

    @Test
    void skal_lukke_brukernotifikasjonsOppgave_nar_eksterne_arena_tiltak_blir_avbrutt() {
        var mockBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        var mockVeileder = MockNavService.createVeileder(mockBruker);
        var serie = ArenaAktivitetskortSerie.of(mockBruker, "MIDL");
        // Opprett FHO
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, serie.getArenaId(), mockVeileder);
        // Opprett ekstern aktivitet og avbryter den
        var aktivitetskortMelding = serie.ny(AktivitetskortStatus.GJENNOMFORES, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntArenaKort(List.of(aktivitetskortMelding));
        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        // Avbryt aktivitet
        var avbruttAktivitet = serie.ny(AktivitetskortStatus.AVBRUTT, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntArenaKort(List.of(avbruttAktivitet));
        brukernotifikasjonAsserts.assertDone(oppgave.key());
    }

    @Test
    void skal_lukke_brukernotifikasjonsOppgave_nar_eksterne_arena_tiltak_blir_avbrutt_men_fho_opprettet_etter_migrering() {
        var mockBruker = navMockService.createHappyBruker(BrukerOptions.happyBruker());
        var mockVeileder = MockNavService.createVeileder(mockBruker);
        // Opprett ekstern aktivitet og avbryter den
        var serie = ArenaAktivitetskortSerie.of(mockBruker, "MIDL");
        var aktivitetskortMelding = serie.ny(AktivitetskortStatus.GJENNOMFORES, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntArenaKort(List.of(aktivitetskortMelding));
        AktivitetDTO opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, mockVeileder, serie.getFunksjonellId());
        // Opprett FHO
        var avtaltMedNavDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(Long.parseLong(opprettetAktivitet.getVersjon()))
                .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(Type.SEND_FORHAANDSORIENTERING)
                        .tekst("lol").lestDato(null).build());
        aktivitetTestService.opprettFHOForInternAktivitet(mockBruker, mockVeileder, avtaltMedNavDTO, Long.parseLong(opprettetAktivitet.getId()));
        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        // Avbryt aktivitet
        var avbruttAktivitet = serie.ny(AktivitetskortStatus.AVBRUTT, ZonedDateTime.now());
        aktivitetTestService.opprettEksterntArenaKort(List.of(avbruttAktivitet));
        brukernotifikasjonAsserts.assertDone(oppgave.key());
    }

    private ConsumerRecord<NokkelInput, OppgaveInput> opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettVarselPaaAktivitet(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");
        final ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord = getSingleRecord(oppgaveConsumer, oppgaveTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        OppgaveInput oppgave = oppgaveRecord.value();

        assertEquals(mockBruker.getOppfolgingsperiodeId().toString(), oppgaveRecord.key().getGrupperingsId());
        assertEquals(mockBruker.getFnr(), oppgaveRecord.key().getFodselsnummer());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgaveRecord;
    }

    private void avsluttOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO, ConsumerRecord<NokkelInput, OppgaveInput> oppgaveRecord) {
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(oppgaveTopic, oppgaveConsumer), "Skal ikke produsere oppgave");
        final ConsumerRecord<NokkelInput, DoneInput> doneRecord = getSingleRecord(doneConsumer, doneTopic, DEFAULT_WAIT_TIMEOUT_DURATION);

        assertEquals(oppgaveRecord.key().getAppnavn(), doneRecord.key().getAppnavn());
        assertEquals(oppgaveRecord.key().getEventId(), doneRecord.key().getEventId());

        assertEquals(mockBruker.getOppfolgingsperiodeId().toString(), doneRecord.key().getGrupperingsId());
        assertEquals(mockBruker.getFnr(), doneRecord.key().getFodselsnummer());
    }

}