package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarbaktivitet.arena.ArenaService;
import no.nav.veilarbaktivitet.avtaltMedNav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtaltMedNav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock.AuthContextRule;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.AktivitetService;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.MetricService;
import org.junit.*;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Random;

import static no.nav.veilarbaktivitet.mock.TestData.*;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.nyMoteAktivitet;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.nyStillingFraNav;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StillingFraNavControllerTest {
    private final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);


    private KvpClient kvpClient = mock(KvpClient.class);
    private KvpService kvpService = new KvpService(kvpClient);
    private MetricService metricService = mock(MetricService.class);
    private ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(database);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDao, meterRegistry);

    private AktivitetService aktivitetService = new AktivitetService(aktivitetDAO, avtaltMedNavService, kvpService, metricService);
    private AuthService authService = mock(AuthService.class);
    private ArenaService arenaService = mock(ArenaService.class);
    private AktivitetAppService appService = new AktivitetAppService(arenaService, authService, aktivitetService, metricService);

    @Mock
    private DelingAvCvDAO delingAvCvDAO;

    private DelingAvCvService delingAvCvService = new DelingAvCvService(delingAvCvDAO, authService, aktivitetService, appService);
    private StillingFraNavController stillingFraNavController = new StillingFraNavController(authService, appService, delingAvCvService);

    @Rule
    public AuthContextRule authContextRule = new AuthContextRule(AuthTestUtils.createAuthContext(UserRole.INTERN, KJENT_SAKSBEHANDLER.get()));

    @Before
    public void setup() {
        when(authService.getAktorIdForPersonBrukerService(any())).thenReturn(Optional.of(KJENT_AKTOR_ID));
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(KJENT_SAKSBEHANDLER));
        when(authService.erInternBruker()).thenReturn(Boolean.TRUE);
        when(authService.erEksternBruker()).thenReturn(Boolean.FALSE);
        when(authService.sjekKvpTilgang(null)).thenReturn(true);
        mockHttpServletRequest.setParameter("fnr", KJENT_IDENT.get());
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void oppdaterKanCvDeles_NavSvarerJA_setterAlleVerdier() {
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(nyStillingFraNav());
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        DelingAvCvDTO delingAvCvDTO = new DelingAvCvDTO(Long.parseLong(aktivitetDTO.getVersjon()), Long.parseLong(aktivitetDTO.getId()), true);

        var resultat = stillingFraNavController.oppdaterKanCvDeles(delingAvCvDTO);
        var resultatJobbannonse = resultat.getStillingFraNavData();
        Assert.assertTrue(resultatJobbannonse.getKanDeles());
        Assert.assertNotNull(resultatJobbannonse.getCvKanDelesTidspunkt());
        Assert.assertEquals(InnsenderData.NAV, resultatJobbannonse.getCvKanDelesAvType());
        Assert.assertEquals(KJENT_SAKSBEHANDLER.get(), resultatJobbannonse.getCvKanDelesAv());
        Assert.assertEquals(AktivitetStatus.GJENNOMFORES, resultat.getStatus());

    }

    @Test
    public void oppdaterKanCvDeles_NavSvarerNEI_setterAlleVerdier() {
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(nyStillingFraNav().withAktorId(KJENT_AKTOR_ID.get()));
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        DelingAvCvDTO delingAvCvDTO = new DelingAvCvDTO(Long.parseLong(aktivitetDTO.getVersjon()), Long.parseLong(aktivitetDTO.getId()), false);

        var resultat = stillingFraNavController.oppdaterKanCvDeles(delingAvCvDTO);
        var resultatJobbannonse = resultat.getStillingFraNavData();
        Assert.assertEquals(AktivitetStatus.AVBRUTT, resultat.getStatus());

        Assert.assertFalse(resultatJobbannonse.getKanDeles());
        Assert.assertNotNull(resultatJobbannonse.getCvKanDelesTidspunkt());
        Assert.assertEquals(InnsenderData.NAV, resultatJobbannonse.getCvKanDelesAvType());
        Assert.assertEquals(KJENT_SAKSBEHANDLER.get(), resultatJobbannonse.getCvKanDelesAv());

    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterKanCvDeles_feilAktivitetstype_feiler() {
        var aktivitetData = aktivitetDAO.opprettNyAktivitet(nyMoteAktivitet().withAktorId(KJENT_AKTOR_ID.get()));
        var delecvDTO = new DelingAvCvDTO(aktivitetData.getVersjon(), aktivitetData.getId(), true);
        stillingFraNavController.oppdaterKanCvDeles(delecvDTO);
    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterKanCvDeles_feilVersjon_feiler() {
        var aktivitetData = aktivitetDAO.opprettNyAktivitet(nyMoteAktivitet().withAktorId(KJENT_AKTOR_ID.get()));
        var delecvDTO = new DelingAvCvDTO(new Random().nextLong(), aktivitetData.getId(), true);
        stillingFraNavController.oppdaterKanCvDeles(delecvDTO);
    }
}
