package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.UserInContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ArenaControllerTest {
    private final UserInContext context = mock(UserInContext.class);
    private final AuthService authService = mock(AuthService.class);
    private final ArenaAktivitetConsumer consumer = mock(ArenaAktivitetConsumer.class);

    private final JdbcTemplate jdbc = LocalH2Database.getDb();
    private final Database db = new Database(jdbc);
    private final ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(db);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ArenaService service = new ArenaService(consumer, fhoDao, authService,meterRegistry);
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
