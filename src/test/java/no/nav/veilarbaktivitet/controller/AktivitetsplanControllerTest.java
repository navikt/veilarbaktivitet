package no.nav.veilarbaktivitet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.common.auth.subject.Subject;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.json.JsonMapper;
import no.nav.veilarbaktivitet.client.KvpClient;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.kafka.KafkaService;
import no.nav.veilarbaktivitet.mock.AktorregisterClientMock;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.mock.SubjectRule;
import no.nav.veilarbaktivitet.service.*;
import no.nav.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;

import static no.nav.common.auth.subject.IdentType.InternBruker;
import static no.nav.common.auth.subject.SsoToken.oidcToken;
import static no.nav.veilarbaktivitet.mock.TestData.KJENT_IDENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AktivitetsplanControllerTest {

    private final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

    private KvpClient kvpClient = mock(KvpClient.class);
    private KafkaService kafkaService = mock(KafkaService.class);
    private FunksjonelleMetrikker funksjonelleMetrikker = mock(FunksjonelleMetrikker.class);

    private AktivitetService aktivitetService = new AktivitetService(aktivitetDAO, kvpClient, kafkaService, funksjonelleMetrikker);
    private AktorregisterClient aktorregisterClient = new AktorregisterClientMock();
    private BrukerService brukerService = new BrukerService(aktorregisterClient);
    private AuthService authService = mock(AuthService.class);
    private ArenaAktivitetConsumer arenaAktivitetConsumer = mock(ArenaAktivitetConsumer.class);
    private AktivitetAppService appService = new AktivitetAppService(arenaAktivitetConsumer, authService, aktivitetService, brukerService, funksjonelleMetrikker);

    private AktivitetsplanController aktivitetController = new AktivitetsplanController(appService, mockHttpServletRequest);
    private static final ObjectMapper objectMapper = JsonMapper.defaultObjectMapper();

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject("testident", InternBruker, oidcToken("token", new HashMap<>())));

    @Before
    public void setup() {
        mockHttpServletRequest.setParameter("fnr", KJENT_IDENT.get());
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    @SneakyThrows
    public void opprettNyAktivitet_isoDatoUTC0_oppretterMedKorrektDato() {
        String jsonBody = "{\"status\":\"PLANLAGT\",\"type\":\"MOTE\",\"" +
                "dato\":\"2020-10-06T14:16:46.000+02:00\",\"" +
                "kanal\":\"OPPMOTE\",\"" +
                "fraDato\":\"2020-10-06T08:00:00.000Z\",\"" +
                "tilDato\":\"2020-10-06T08:45:00.000Z\"}";
        AktivitetDTO aktivitet = objectMapper.readValue(jsonBody, AktivitetDTO.class);
        AktivitetDTO result = aktivitetController.opprettNyAktivitet(aktivitet, false);
        assertEquals("2020-10-06T10:00+02:00[Europe/Oslo]", result.fraDato.toString());
    }

    @Test
    @SneakyThrows
    public void opprettNyAktivitet_isoDatoUTC2_oppretterMedKorrektDato() {
        String jsonBody = "{\"status\":\"PLANLAGT\",\"type\":\"MOTE\",\"" +
                "dato\":\"2020-10-06T14:16:46.000+02:00\",\"" +
                "kanal\":\"OPPMOTE\",\"" +
                "fraDato\":\"2020-10-06T08:00:00+02\",\"" +
                "tilDato\":\"2020-10-06T08:45:00+02\"}";
        AktivitetDTO aktivitet = objectMapper.readValue(jsonBody, AktivitetDTO.class);
        AktivitetDTO resultat = aktivitetController.opprettNyAktivitet(aktivitet, false);
        assertEquals("2020-10-06T08:00+02:00[Europe/Oslo]", resultat.fraDato.toString());
    }

    @Test
    @SneakyThrows
    public void opprettNyAktivitet_isoDatoUTC1_oppretterMedKorrektDato() {
        String jsonBody = "{\"status\":\"PLANLAGT\",\"type\":\"MOTE\",\"" +
                "dato\":\"2020-10-06T14:16:46.000+02:00\",\"" +
                "kanal\":\"OPPMOTE\",\"" +
                "fraDato\":\"2020-10-06T08:00:00+01\",\"" +
                "tilDato\":\"2020-10-06T08:45:00+01\"}";
        AktivitetDTO aktivitet = objectMapper.readValue(jsonBody, AktivitetDTO.class);
        AktivitetDTO resultat = aktivitetController.opprettNyAktivitet(aktivitet, false);
        assertEquals("2020-10-06T09:00+02:00[Europe/Oslo]", resultat.fraDato.toString());
    }

}
