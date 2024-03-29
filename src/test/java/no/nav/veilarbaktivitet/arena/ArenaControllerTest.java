package no.nav.veilarbaktivitet.arena;

import io.getunleash.Unleash;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMetrikker;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
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
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode;
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeDAO;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArenaControllerTest {
    private final UserInContext context = mock(UserInContext.class);
    private final IAuthService authService = mock(AuthService.class);
    private final PersonService personService = mock(PersonService.class);
    private final AktivitetDAO aktivitetDAO = mock(AktivitetDAO.class);
    private final SistePeriodeService sistePeriodeService = mock(SistePeriodeService.class);
    private final Nivaa4Client nivaa4Client = mock(Nivaa4Client.class);
    private final ManuellStatusV2Client manuellStatusClient = mock(ManuellStatusV2Client.class);

    private final VeilarbarenaClient veilarbarenaClient = mock(VeilarbarenaClient.class);
    private final String aktivitetsplanBasepath = "http://localhost:3000";

    private final JdbcTemplate jdbc = LocalH2Database.getDb();
    private final Database db = new Database(jdbc);
    private final BrukerNotifikasjonDAO notifikasjonArenaDAO = new BrukerNotifikasjonDAO(new NamedParameterJdbcTemplate(jdbc));
    private final BrukernotifikasjonService brukernotifikasjonArenaAktivitetService = new BrukernotifikasjonService(personService, sistePeriodeService, notifikasjonArenaDAO, nivaa4Client, manuellStatusClient, aktivitetsplanBasepath, aktivitetDAO);
    private final ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(db.getNamedJdbcTemplate());
    private final IdMappingDAO idMappingDAO = new IdMappingDAO(new NamedParameterJdbcTemplate(jdbc));
    private final Unleash unleash = mock(Unleash.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AktivitetskortMetrikker aktivitetskortMetrikker = new AktivitetskortMetrikker(meterRegistry);
    private final MigreringService migreringService = new MigreringService(unleash, idMappingDAO, aktivitetskortMetrikker);
    private final OppfolgingsperiodeDAO oppfolgingsperiodeDAO = mock(OppfolgingsperiodeDAO.class);
    private final ArenaService arenaService = new ArenaService(fhoDao, meterRegistry, brukernotifikasjonArenaAktivitetService, veilarbarenaClient, idMappingDAO, personService);
    private final ArenaController controller = new ArenaController(context, authService, arenaService, idMappingDAO, aktivitetDAO, migreringService, oppfolgingsperiodeDAO);

    private final Person.AktorId aktorid = Person.aktorId("12345678");
    private final Person.Fnr fnr = Person.fnr("987654321");
    private final Person.Fnr ikkeTilgangFnr = Person.fnr("10108000");
    private final Person.AktorId ikkeTilgangAktorid = Person.aktorId("00080101");

    private final NavIdent veilederIdent = NavIdent.of("Z123456");

    private final ForhaandsorienteringDTO forhaandsorientering = ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build();

    @BeforeEach
    void cleanup() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangTilEnhet(any());
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangTilPerson(any());

        doNothing()
                .when(authService)
                .sjekkTilgangTilEnhet(null);
        doNothing()
                .when(authService)
                .sjekkTilgangTilPerson(Person.aktorId(aktorid.get()).eksternBrukerId());


        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangTilPerson(any());

        doNothing()
                .when(authService)
                .sjekkTilgangTilPerson(fnr.eksternBrukerId());

        when(authService.erInternBruker()).thenReturn(true);
        when(personService.getAktorIdForPersonBruker(fnr)).thenReturn(Optional.of(aktorid));
        when(personService.getAktorIdForPersonBruker(ikkeTilgangFnr)).thenReturn(Optional.of(ikkeTilgangAktorid));
        when(context.getFnr()).thenReturn(Optional.of(fnr));
        when(context.getAktorId()).thenReturn(aktorid);
        when(manuellStatusClient.get(aktorid)).thenReturn(Optional.of(new ManuellStatusV2DTO(false, new ManuellStatusV2DTO.KrrStatus(true, false))));
        when(nivaa4Client.get(aktorid)).thenReturn(Optional.of(Nivaa4DTO.builder().harbruktnivaa4(true).build()));
        when(sistePeriodeService.hentGjeldendeOppfolgingsperiodeMedFallback(aktorid)).thenReturn(UUID.randomUUID());
        when(personService.getAktorIdForPersonBruker(fnr)).thenReturn(Optional.of(aktorid));
        when(oppfolgingsperiodeDAO.getByAktorId(aktorid)).thenReturn(List.of(new Oppfolgingsperiode(
            aktorid.get(),
            UUID.randomUUID(),
            ZonedDateTime.now().minusYears(6),
            null
        )));
    }

    @BeforeEach
    void cleanupBetweenTests() {
        DbTestUtils.cleanupTestDb(jdbc);
        when(authService.getInnloggetVeilederIdent()).thenReturn(veilederIdent);

    }

    @Test
    void harTiltakSkalReturnereFalseUtenTiltak() {
        assertFalse(controller.hentHarTiltak());
    }

    @Test
    void harTiltakSkalReturnereTrueMedTiltak() {
        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO().setGruppeaktiviteter(List.of(createGruppeaktivitet()))));

        assertTrue(controller.hentHarTiltak());
    }

    @Test
    void sendForhaandsorienteringSkalFeileUtenForhaandsorientering() {
        ArenaId arenaId = new ArenaId("ARENATAAktivitetId");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(null, arenaId));
        assertEquals("forhaandsorientering kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendForhaandsorienteringSkalFeileUtenForhaandsorienteringsType() {
        ForhaandsorienteringDTO fho = ForhaandsorienteringDTO.builder().build();
        ArenaId arenaId = new ArenaId("ARENATAAktivitetId");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(fho, arenaId));
        assertEquals("forhaandsorientering.type kan ikke være null", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendForhaandsorienteringSkalFeileUtenArenaAktivitet() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(null, null));
        assertEquals("arenaaktivitetId kan ikke være null eller tom", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendForhaandsorienteringSkalFeileHvisArenaAktivitetenIkkeFinnes() {
        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()));

        ArenaId arenaId = new ArenaId("ARENAGA123");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, arenaId));
        assertEquals("Aktiviteten finnes ikke", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendForhaandsorienteringSkalFeileHvisAleredeSendtForhaandsorientering() {
        AktiviteterDTO.Gruppeaktivitet medFho = createGruppeaktivitet();

        AktiviteterDTO.Tiltaksaktivitet utenFho = createTiltaksaktivitet();

        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()
                        .setGruppeaktiviteter(List.of(medFho))
                        .setTiltaksaktiviteter(List.of(utenFho))));

        ArenaId medFhoId = medFho.getAktivitetId();

        controller.opprettFHO(forhaandsorientering, medFhoId);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, medFhoId));
        assertEquals("Det er allerede sendt forhaandsorientering på aktiviteten", exception.getReason());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void sendForhaandsorienteringSkalreturnereAktivitetMedForhaandsorientering() {

        AktiviteterDTO.Gruppeaktivitet medFho = createGruppeaktivitet();

        AktiviteterDTO.Tiltaksaktivitet utenFho = createTiltaksaktivitet();

        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()
                        .setGruppeaktiviteter(List.of(medFho))
                        .setTiltaksaktiviteter(List.of(utenFho))));

        ArenaAktivitetDTO arenaAktivitetDTO = controller.opprettFHO(forhaandsorientering, medFho.getAktivitetId());
        Optional<ArenaAktivitetDTO> gruppeAktivitet = arenaService.hentAktiviteter(fnr).stream().filter(a -> a.getType().equals(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET)).findAny();
        assertTrue(gruppeAktivitet.isPresent());
        ArenaAktivitetDTO gr = gruppeAktivitet.get();

        assertEquals(gr, arenaAktivitetDTO);
        assertEquals(gr.getForhaandsorientering().getTekst(), forhaandsorientering.getTekst());
    }

    private String getRandomString() {
        return String.valueOf(new Random().nextInt());
    }

    @Test
    void sendForhaandsorienteringSkalOppdaterehentArenaAktiviteter() {
        AktiviteterDTO.Gruppeaktivitet medFho = createGruppeaktivitet();
        AktiviteterDTO.Tiltaksaktivitet utenFho = createTiltaksaktivitet();
        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()
                        .setGruppeaktiviteter(List.of(medFho))
                        .setTiltaksaktiviteter(List.of(utenFho))));
        controller.opprettFHO(forhaandsorientering, medFho.getAktivitetId());
        List<ArenaAktivitetDTO> arenaAktivitetDTOS = controller.hentArenaAktiviteter();
        Assertions.assertThat(arenaAktivitetDTOS)
                .hasSize(2)
                .anyMatch(a -> a.getType().equals(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET) && a.getId().equals(medFho.getAktivitetId().id()) && a.getForhaandsorientering().getTekst().equals(forhaandsorientering.getTekst()))
                .anyMatch(a -> a.getType().equals(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET) && a.getId().equals(utenFho.getAktivitetId().id()));
    }

    @Test
    void hentArenaAktiviteterSkalReturnereArenaAktiviteter() {

        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()
                        .setGruppeaktiviteter(List.of(
                                createGruppeaktivitet(),
                                createGruppeaktivitet()
                        ))));

        List<ArenaAktivitetDTO> arenaAktivitetDTOS = controller.hentArenaAktiviteter();

        Assertions.assertThat(arenaAktivitetDTOS).hasSize(2);
    }

    @Test
    void hentArenaAktiviteterSkalReturnereTomListeNarArenaGirTomListe() {
        when(veilarbarenaClient.hentAktiviteter(fnr)).thenReturn(Optional.empty());

        List<ArenaAktivitetDTO> arenaAktivitetDTOS = controller.hentArenaAktiviteter();

        Assertions.assertThat(arenaAktivitetDTOS).isEmpty();
    }

    @Test
    void markerForhaandsorienteringSomLestSkalOppdatereArenaAktivitet() {
        Date start = new Date();

        AktiviteterDTO.Utdanningsaktivitet utdanningsaktivitet = createUtdanningsaktivitet();

        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO().setUtdanningsaktiviteter(List.of(utdanningsaktivitet))));

        ArenaAktivitetDTO sendtAktivitet = controller.opprettFHO(forhaandsorientering, utdanningsaktivitet.getAktivitetId());

        assertNull(sendtAktivitet.getForhaandsorientering().getLestDato());

        ArenaAktivitetDTO lestAktivitet = controller.lest(new ArenaId(sendtAktivitet.getId()));

        Date stopp = new Date();
        Date lest = lestAktivitet.getForhaandsorientering().getLestDato();

        assertNotNull(lest);

        assertTrue(start.before(lest) || start.equals(lest));
        assertTrue(stopp.after(lest) || stopp.equals(lest));
    }







    @Test
    void tilgangskontrollPaaSendForhaandsorienteringSkalFinnes() {
        AktiviteterDTO.Gruppeaktivitet medFho = new AktiviteterDTO.Gruppeaktivitet().setAktivitetId(new ArenaId("ARENAGA" + getRandomString()));

        AktiviteterDTO.Tiltaksaktivitet utenFho = new AktiviteterDTO.Tiltaksaktivitet().setAktivitetId(new ArenaId("ARENATA" + getRandomString()));

        when(veilarbarenaClient.hentAktiviteter(fnr))
                .thenReturn(Optional.of(new AktiviteterDTO()
                        .setGruppeaktiviteter(List.of(medFho))
                        .setTiltaksaktiviteter(List.of(utenFho))));

        when(context.getFnr()).thenReturn(Optional.of(ikkeTilgangFnr));


        ArenaId arenaAktivitetId = medFho.getAktivitetId();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> controller.opprettFHO(forhaandsorientering, arenaAktivitetId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private AktiviteterDTO.Tiltaksaktivitet createTiltaksaktivitet() {
        return new AktiviteterDTO.Tiltaksaktivitet()
                .setDeltakerStatus(VeilarbarenaMapper.ArenaStatus.GJENN.name())
                .setTiltaksnavn(VeilarbarenaMapper.VANLIG_AMO_NAVN)
                .setStatusSistEndret(LocalDate.now().minusYears(7))
                .setDeltakelsePeriode(
                        new AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode()
                                // Dette er vanlig på VASV tiltakene, starter før aktivitetplanen, slutter
                                // mange år frem i tid
                                .setFom(LocalDate.now().minusYears(7))
                                .setTom(LocalDate.now().plusYears(7))
                )
                .setAktivitetId(new ArenaId("ARENATA" + getRandomString()));
    }

    private AktiviteterDTO.Gruppeaktivitet createGruppeaktivitet() {
        return new AktiviteterDTO.Gruppeaktivitet()
                .setMoteplanListe(List.of(
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(LocalDate.ofInstant(Instant.now().minus(7, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(LocalDate.ofInstant(Instant.now().minus(7, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setSluttKlokkeslett("12:00:00")
                                ,
                                new AktiviteterDTO.Gruppeaktivitet.Moteplan()
                                        .setStartDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setStartKlokkeslett("10:00:00")
                                        .setSluttDato(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                                        .setSluttKlokkeslett("12:00:00")
                        )

                )
                .setAktivitetId(new ArenaId("ARENATA" + getRandomString()));
    }

    private AktiviteterDTO.Utdanningsaktivitet createUtdanningsaktivitet() {
        AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode periode = new AktiviteterDTO.Utdanningsaktivitet.AktivitetPeriode()
                .setFom(LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault()))
                .setTom(LocalDate.ofInstant(Instant.now().plus(4, ChronoUnit.DAYS), ZoneId.systemDefault()));

        return new AktiviteterDTO.Utdanningsaktivitet()
                .setAktivitetId(new ArenaId("ARENAUA" + getRandomString()))
                .setAktivitetPeriode(periode);
    }
}
