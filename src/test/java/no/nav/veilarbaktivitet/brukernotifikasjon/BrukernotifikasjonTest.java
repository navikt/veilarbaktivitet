package no.nav.veilarbaktivitet.brukernotifikasjon;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
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
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.person.Person;
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
    @Autowired
    BrukernotifikasjonService brukernotifikasjonService;

    @Autowired
    AvsluttBrukernotifikasjonCron avsluttBrukernotifikasjonCron;

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Autowired
    KafkaTestService kafkaTestService;

    @Value("${topic.ut.brukernotifikasjon.brukervarsel}")
    String brukervarselTopic;

    @Value("${topic.inn.brukernotifikasjon.brukervarselHendelse}")
    String brukervarselHendelseTopic;

    Consumer<String, String> brukerVarselHendelseConsumer;
    Consumer<String, String> brukerVarselConsumer;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Value("${app.env.aktivitetsplan.basepath}")
    String basepath;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    BrukernotifikasjonAsserts getOrCreateBrukernotifikasjonAsserts() {
        if (brukernotifikasjonAsserts != null) {
            return brukernotifikasjonAsserts;
        }
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
        return brukernotifikasjonAsserts;
    }

    @BeforeEach
    void setUp() {
        brukernotifikasjonAsserts = getOrCreateBrukernotifikasjonAsserts();
        brukerVarselConsumer = brukernotifikasjonAsserts.getBrukervarselConsumer().getConsumer();
        brukerVarselHendelseConsumer = kafkaTestService.createStringStringConsumer(brukervarselHendelseTopic);
        DbTestUtils.cleanupTestDb(jdbc.getJdbcTemplate());

        when(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true);
    }

    @AfterEach
    void assertNoUnkowns() {
        brukerVarselConsumer.unsubscribe();
        brukerVarselHendelseConsumer.unsubscribe();

        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @SneakyThrows
    @Test
    void happy_case_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        final OpprettVarselDto oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO);
        oppgaveErIOutbox(oppgaveRecord);
        avsluttOppgave(aktivitetDTO, oppgaveRecord);
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

        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV,
                epostTitel,
                epostTekst,
                SMSTekst
            )
        );
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        var oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(Person.fnr(mockBruker.getFnr()));

        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselHendelseTopic, brukerVarselHendelseConsumer), "Skal ikke produsert done meldinger");
        assertEquals(mockBruker.getFnr(), oppgave.getIdent());
        assertEquals(epostTitel, oppgave.getEksternVarsling().getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEksternVarsling().getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getEksternVarsling().getSmsVarslingstekst());
        oppgaveErIOutbox(oppgave);
        avsluttOppgave(aktivitetDTO, oppgave);
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

        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.MOTE_SMS,
                epostTitel,
                epostTekst,
                SMSTekst)
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselHendelseTopic, brukerVarselHendelseConsumer), "Skal ikke produsert done meldinger");
        var oppgave = brukernotifikasjonAsserts.assertBeskjedSendt(Person.fnr(mockBruker.getFnr()));
        assertEquals(epostTitel, oppgave.getEksternVarsling().getEpostVarslingstittel());
        assertEquals(epostTekst, oppgave.getEksternVarsling().getEpostVarslingstekst());
        assertEquals(SMSTekst, oppgave.getEksternVarsling().getSmsVarslingstekst());
    }

    @SneakyThrows
    @Test
    void skal_sendeBeskjed() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.MOTE_SMS, null, null, null)
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();

        OpprettVarselDto beskjed = brukernotifikasjonAsserts.assertBeskjedSendt(Person.fnr(mockBruker.getFnr()));

        assertEquals(mockBruker.getFnr(), beskjed.getIdent());
        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), beskjed.getLink());
    }

    @Test
    void skal_ikke_produsere_meldinger_for_avsluttet_oppgave() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(Long.parseLong(aktivitetDTO.getId()), Long.parseLong(aktivitetDTO.getVersjon()), mockBruker.getAktorId(), "Testvarsel", VarselType.STILLING_FRA_NAV, null, null, null));
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselTopic, brukerVarselConsumer), "Skal ikke produsert oppgave eller inaktivering meldinger");
//        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselHendelseTopic, brukerVarselHendelseConsumer), "Skal ikke produsert done meldinger");
    }

    @Test
    void skal_ikke_sende_meldinger_for_avbrutte_aktiviteter() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);
        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV, null, null, null
        ));

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

        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselTopic, brukerVarselConsumer), "Skal ikke produsert oppgave meldinger");
        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselHendelseTopic, brukerVarselHendelseConsumer), "Skal ikke produsert done meldinger");

        AktivitetsplanDTO aktivitetsplanDTO = aktivitetTestService.hentAktiviteterForFnr(mockBruker);
        AktivitetDTO skalIkkeVaereOppdatert = AktivitetTestService.finnAktivitet(aktivitetsplanDTO, avbruttAktivitet.getId());

        assertEquals(avbruttAktivitet, skalIkkeVaereOppdatert);
    }

    @Test
    void skal_lukke_brukernotifikasjonsOppgave_nar_aktivitet_blir_avbrutt() {
        // Given
        MockBruker mockBruker = MockNavService.createHappyBruker();
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        AktivitetDTO aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes);

        // When
        var oppgave = opprettOppgave(mockBruker, aktivitetDTO);
        oppgaveErIOutbox(oppgave);
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

        // Then
//        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(brukervarselTopic, brukerVarselConsumer), "Skal ikke produsert oppgave meldinger");
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(oppgave.getVarselId());
    }


    private void oppgaveErIOutbox(OpprettVarselDto varselDto) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("eventId", varselDto.getVarselId());
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
        final ConsumerRecord<String, String> oppgaveRecord = getSingleRecord(brukerVarselConsumer, brukervarselTopic, DEFAULT_WAIT_TIMEOUT_DURATION);
        var lenke = JsonUtils.fromJson(oppgaveRecord.value(), OpprettVarselDto.class).getLink();
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

        var oppgaveRecord = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        var lenke = oppgaveRecord.getLink();
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
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(oppgave.getVarselId());
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
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(oppgave.getVarselId());
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
        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(oppgave.getVarselId());
    }

    private OpprettVarselDto opprettOppgave(MockBruker mockBruker, AktivitetDTO aktivitetDTO) {
        brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                Long.parseLong(aktivitetDTO.getId()),
                Long.parseLong(aktivitetDTO.getVersjon()),
                mockBruker.getAktorId(),
                "Testvarsel",
                VarselType.STILLING_FRA_NAV, null, null, null)
        );

        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        OpprettVarselDto oppgave = brukernotifikasjonAsserts.assertOppgaveSendt(Person.fnr(mockBruker.getFnr()));

        assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.getId(), oppgave.getLink());
        return oppgave;
    }

    private void avsluttOppgave(AktivitetDTO aktivitetDTO, OpprettVarselDto oppgave) {
        brukernotifikasjonService.setDone(Long.parseLong(aktivitetDTO.getId()), VarselType.STILLING_FRA_NAV);
        sendBrukernotifikasjonCron.sendBrukernotifikasjoner();
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner();

        brukernotifikasjonAsserts.assertInaktivertMeldingErSendt(oppgave.getVarselId());
    }

}