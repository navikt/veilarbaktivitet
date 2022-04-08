package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukerNotifikasjonDAO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client;
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2DTO;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4DTO;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ArenaControllerTest {
    private final UserInContext context = mock(UserInContext.class);
    private final AuthService authService = mock(AuthService.class);
    private final ArenaAktivitetConsumer consumer = mock(ArenaAktivitetConsumer.class);
    private final PersonService personService = mock(PersonService.class);
    private final SistePeriodeService sistePeriodeService = mock(SistePeriodeService.class);
    private final Nivaa4Client nivaa4Client = mock(Nivaa4Client.class);
    private final ManuellStatusV2Client manuellStatusClient = mock(ManuellStatusV2Client.class);
    private String aktivitetsplanBasepath = "http://localhost:3000";

    private final JdbcTemplate jdbc = LocalH2Database.getDb();
    private final Database db = new Database(jdbc);
    private final BrukerNotifikasjonDAO notifikasjonArenaDAO = new BrukerNotifikasjonDAO(new NamedParameterJdbcTemplate(jdbc));
    private final BrukernotifikasjonService brukernotifikasjonArenaAktivitetService = new BrukernotifikasjonService(personService, sistePeriodeService, notifikasjonArenaDAO, nivaa4Client, manuellStatusClient, aktivitetsplanBasepath);
    private final ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(db);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ArenaService service = new ArenaService(consumer, fhoDao, authService,meterRegistry, brukernotifikasjonArenaAktivitetService);
    private final ArenaController controller = new ArenaController(context, authService, service);

    private final Person.AktorId aktorid = Person.aktorId("12345678");
    private final Person.Fnr fnr = Person.fnr("987654321");
    private final Person.Fnr ikkeTilgangFnr = Person.fnr("10108000");
    private final Person.AktorId ikkeTilgangAktorid = Person.aktorId("00080101");

    private final ForhaandsorienteringDTO forhaandsorientering = ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build();

    @Before
    public void cleanup() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangOgInternBruker(any(), any());

        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid.get(), null);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangTilPerson((Person) any());

        doNothing()
                .when(authService)
                .sjekkTilgangTilPerson(fnr);

        when(authService.getInnloggetBrukerIdent())
                .thenReturn(Optional.of("Z12345"));

        when(authService.erInternBruker()).thenReturn(true);
        when(authService.getAktorIdForPersonBrukerService(fnr)).thenReturn(Optional.of(aktorid));
        when(authService.getAktorIdForPersonBrukerService(ikkeTilgangFnr)).thenReturn(Optional.of(ikkeTilgangAktorid));
        when(context.getFnr()).thenReturn(Optional.of(fnr));
        when(manuellStatusClient.get(aktorid)).thenReturn(Optional.of(new ManuellStatusV2DTO(false, new ManuellStatusV2DTO.KrrStatus(true, false))));
        when(nivaa4Client.get(aktorid)).thenReturn(Optional.of(Nivaa4DTO.builder().harbruktnivaa4(true).build()));
        when(sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorid)).thenReturn(UUID.randomUUID());
        when(personService.getAktorIdForPersonBruker(fnr)).thenReturn(Optional.of(aktorid));
    }

    @Before
    public void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);

    }

    @Test
    public void harTiltakSkalReturnereFalseUtenTiltak() {
        assertFalse(controller.hentHarTiltak());
    }

    @Test
    public void harTiltakSkalReturnereTrueMedTiltak() {
        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(new ArenaAktivitetDTO().setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET)));

        assertTrue(controller.hentHarTiltak());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileUtenForhaandsorientering() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(null, "arenaAktivitetId"));
        assertEquals( "forhaandsorientering kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileUtenForhaandsorienteringsType() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(ForhaandsorienteringDTO.builder().build(), "arenaAktivitetId"));
        assertEquals( "forhaandsorientering.type kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileUtenArenaAktivitet() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(null, null));
        assertEquals( "arenaaktivitetId kan ikke være null eller tom", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileHvisArenaAktivitetenIkkeFinnes() {
        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(new ArenaAktivitetDTO()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, "Arena123"));
        assertEquals( "Aktiviteten finnes ikke", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileHvisAleredeSendtForhaandsorientering() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId(getRandomString());
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId(getRandomString());

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        controller.opprettFHO(forhaandsorientering, tilFho.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, tilFho.getId()));
        assertEquals( "Det er allerede sendt forhaandsorientering på aktiviteten", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalreturnereAktivitetMedForhaandsorientering() {
        ArenaAktivitetDTO medFHO = new ArenaAktivitetDTO();
        medFHO.setId("tilFho");
        medFHO.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO utenFHO = new ArenaAktivitetDTO();
        utenFHO.setId("ikkeTilFho");

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(medFHO, utenFHO));

        ArenaAktivitetDTO arenaAktivitetDTO = controller.opprettFHO(forhaandsorientering, medFHO.getId());

        assertEquals(medFHO.setForhaandsorientering(forhaandsorientering), arenaAktivitetDTO);
    }

    private String getRandomString(){
        return String.valueOf(new Random().nextInt());
    }

    @Test
    public void sendForhaandsorienteringSkalOppdaterehentArenaAktiviteter() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId(getRandomString());
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId(getRandomString());

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        controller.opprettFHO(forhaandsorientering, tilFho.getId());
        List<ArenaAktivitetDTO> arenaAktivitetDTOS = controller.hentArenaAktiviteter();

        List<ArenaAktivitetDTO> forventet = List.of(tilFho.setForhaandsorientering(forhaandsorientering), ikkeTilFho);

        assertEquals(forventet, arenaAktivitetDTOS);
    }

    @Test
    public void hentArenaAktiviteterSkalReturnereArenaAktiviteter() {

        ArenaAktivitetDTO t1 = new ArenaAktivitetDTO();
        t1.setId(getRandomString());
        ArenaAktivitetDTO t2 = new ArenaAktivitetDTO();
        t2.setId(getRandomString());

        List<ArenaAktivitetDTO> tiltak = List.of(t1, t2);

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(tiltak);

        assertEquals(tiltak, controller.hentArenaAktiviteter());
    }

    @Test
    public void markerForhaandsorienteringSomLestSkalOppdatereArenaAktivitet() {
        Date start = new Date();
        ArenaAktivitetDTO a1 = new ArenaAktivitetDTO();
        a1.setId(getRandomString());
        a1.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(a1));

        ArenaAktivitetDTO sendtAktivitet = controller.opprettFHO(forhaandsorientering, a1.getId());

        assertNull(sendtAktivitet.getForhaandsorientering().getLestDato());

        ArenaAktivitetDTO lestAktivitet = controller.lest(sendtAktivitet.getId());

        Date stopp = new Date();
        Date lest = lestAktivitet.getForhaandsorientering().getLestDato();

        assertNotNull(lest);

        assertTrue(start.before(lest) || start.equals(lest));
        assertTrue(stopp.after(lest) || stopp.equals(lest));
    }

    @Test
    public void tilgangskontrollPaaMarkerSomLestSkalFinnes() {
        when(context.getFnr()).thenReturn(Optional.of(ikkeTilgangFnr));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.lest("errorId"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void tilgangskontrollPaaHentArenaAktiviteterSkalFinnes() {
        ArenaAktivitetDTO t1 = new ArenaAktivitetDTO();
        t1.setId(getRandomString());
        ArenaAktivitetDTO t2 = new ArenaAktivitetDTO();
        t2.setId(getRandomString());

        List<ArenaAktivitetDTO> tiltak = List.of(t1, t2);

        when(consumer.hentArenaAktiviteter(ikkeTilgangFnr))
                .thenReturn(tiltak);

        when(context.getFnr()).thenReturn(Optional.of(ikkeTilgangFnr));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, controller::hentArenaAktiviteter);

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void tilgangskontrollPaaHarTiltakSkalFinnes() {
        when(context.getFnr()).thenReturn(Optional.of(ikkeTilgangFnr));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, controller::hentHarTiltak);

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void tilgangskontrollPaaSendForhaandsorienteringSkalFinnes() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId("tilFho");
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId("ikkeTilFho");

        when(consumer.hentArenaAktiviteter(ikkeTilgangFnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        when(context.getFnr()).thenReturn(Optional.of(ikkeTilgangFnr));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, tilFho.getId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }
}
