package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtaltMedNav.Type;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.UserInContext;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ArenaControllerTest {
    private final UserInContext context = mock(UserInContext.class);
    private final AuthService authService = mock(AuthService.class);
    private final ArenaAktivitetConsumer consumer = mock(ArenaAktivitetConsumer.class);

    private final JdbcTemplate jdbc = LocalH2Database.getDb();
    private final ArenaForhaandsorienteringDAO dao = new ArenaForhaandsorienteringDAO(new Database(jdbc));
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ArenaService service = new ArenaService(consumer, dao, authService,meterRegistry);
    private final ArenaController controller = new ArenaController(context, authService, service);

    private final Person.AktorId aktorid = Person.aktorId("12345678");
    private final Person.Fnr fnr = Person.fnr("987654321");
    private final Person.Fnr ikkeTilgangFnr = Person.fnr("10108000");
    private final Person.AktorId ikkeTilgangAktorid = Person.aktorId("00080101");

    private final Forhaandsorientering forhaandsorientering = Forhaandsorientering.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build();

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbc);

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
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(null, "arenaAktivitetId"));
        assertEquals( "forhaandsorientering kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileUtenForhaandsorienteringsType() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(Forhaandsorientering.builder().build(), "arenaAktivitetId"));
        assertEquals( "forhaandsorientering kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileUtenArenaAktivitet() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(null, null));
        assertEquals( "arenaaktivitetId kan ikke være null eller tom", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileHvisArenaAktivitetenIkkeFinnes() {
        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(new ArenaAktivitetDTO()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(forhaandsorientering, "Arena123"));
        assertEquals( "Aktiviteten finnes ikke", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalFeileHvisAleredeSendtForhaandsorientering() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId("tilFho");
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId("ikkeTilFho");

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        controller.sendForhaandsorientering(forhaandsorientering, tilFho.getId());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(forhaandsorientering, tilFho.getId()));
        assertEquals( "Det er allerede sendt forhaandsorientering på aktiviteten", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void sendForhaandsorienteringSkalreturnereAktivitetMedForhaandsorientering() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId("tilFho");
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId("ikkeTilFho");

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        ArenaAktivitetDTO arenaAktivitetDTO = controller.sendForhaandsorientering(forhaandsorientering, tilFho.getId());

        assertEquals(tilFho.setForhaandsorientering(forhaandsorientering), arenaAktivitetDTO);
    }

    @Test
    public void sendForhaandsorienteringSkalOppdaterehentArenaAktiviteter() {
        ArenaAktivitetDTO tilFho = new ArenaAktivitetDTO();
        tilFho.setId("tilFho");
        tilFho.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        ArenaAktivitetDTO ikkeTilFho = new ArenaAktivitetDTO();
        ikkeTilFho.setId("ikkeTilFho");

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(tilFho, ikkeTilFho));

        controller.sendForhaandsorientering(forhaandsorientering, tilFho.getId());
        List<ArenaAktivitetDTO> arenaAktivitetDTOS = controller.hentArenaAktiviteter();

        List<ArenaAktivitetDTO> forventet = List.of(tilFho.setForhaandsorientering(forhaandsorientering), ikkeTilFho);

        assertEquals(forventet, arenaAktivitetDTOS);
    }

    @Test
    public void hentArenaAktiviteterSkalReturnereArenaAktiviteter() {

        ArenaAktivitetDTO t1 = new ArenaAktivitetDTO();
        t1.setId("tilFho");
        ArenaAktivitetDTO t2 = new ArenaAktivitetDTO();
        t2.setId("ikkeTilFho");

        List<ArenaAktivitetDTO> tiltak = List.of(t1, t2);

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(tiltak);

        assertEquals(tiltak, controller.hentArenaAktiviteter());
    }

    @Test
    public void markerForhaandsorienteringSomLestSkalOppdatereArenaAktivitet() {
        Date start = new Date();
        ArenaAktivitetDTO a1 = new ArenaAktivitetDTO();
        a1.setId("settTilLest");
        a1.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);

        when(consumer.hentArenaAktiviteter(fnr))
                .thenReturn(List.of(a1));

        ArenaAktivitetDTO sendtAktivitet = controller.sendForhaandsorientering(forhaandsorientering, a1.getId());

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
        t1.setId("tilFho");
        ArenaAktivitetDTO t2 = new ArenaAktivitetDTO();
        t2.setId("ikkeTilFho");

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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.sendForhaandsorientering(forhaandsorientering, tilFho.getId()));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }
}
