package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.kvp.v2.KvpV2Client;
import no.nav.veilarbaktivitet.mock.AuthContextRule;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.InnsenderData;
import org.junit.*;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

import static no.nav.veilarbaktivitet.mock.TestData.*;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StillingFraNavControllerTest {
    public static final Date AVTALT_DATO = new Date(2021, Calendar.APRIL, 30);
    private final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

    private final KvpV2Client kvpClient = mock(KvpV2Client.class);
    private final KvpService kvpService = new KvpService(kvpClient);
    private final MetricService metricService = mock(MetricService.class);
    private final ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(database);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SistePeriodeService sistePeriodeService = mock(SistePeriodeService.class);
    private final AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDao, meterRegistry);

    private final AktivitetService aktivitetService = new AktivitetService(aktivitetDAO, avtaltMedNavService, kvpService, metricService, sistePeriodeService);
    private final AuthService authService = mock(AuthService.class);
    private final AktivitetAppService appService = new AktivitetAppService(authService, aktivitetService, metricService);

    @Mock
    private DelingAvCvDAO delingAvCvDAO;

    private final DelingAvCvService delingAvCvService = new DelingAvCvService(aktivitetDAO, delingAvCvDAO, authService, aktivitetService, mock(StillingFraNavProducerClient.class), mock(BrukernotifikasjonService.class), mock(StillingFraNavMetrikker.class));
    private final StillingFraNavController stillingFraNavController = new StillingFraNavController(authService, appService, delingAvCvService);

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
        DelingAvCvDTO delingAvCvDTO = new DelingAvCvDTO(Long.parseLong(aktivitetDTO.getVersjon()), true, AVTALT_DATO);

        var resultat = stillingFraNavController.oppdaterKanCvDeles(aktivitetData.getId(), delingAvCvDTO);
        var resultatStilling = resultat.getStillingFraNavData();

        Assert.assertTrue(resultatStilling.getCvKanDelesData().getKanDeles());
        Assert.assertNotNull(resultatStilling.getCvKanDelesData().getEndretTidspunkt());
        Assert.assertEquals(InnsenderData.NAV, resultatStilling.getCvKanDelesData().getEndretAvType());
        Assert.assertEquals(KJENT_SAKSBEHANDLER.get(), resultatStilling.getCvKanDelesData().getEndretAv());
        Assert.assertEquals(AktivitetStatus.GJENNOMFORES, resultat.getStatus());
        Assert.assertEquals(AVTALT_DATO, resultatStilling.getCvKanDelesData().avtaltDato);

    }

    @Test
    public void oppdaterKanCvDeles_NavSvarerNEI_setterAlleVerdier() {
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(nyStillingFraNavMedCVKanDeles().withAktorId(KJENT_AKTOR_ID.get()));
        AktivitetDTO aktivitetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        DelingAvCvDTO delingAvCvDTO = new DelingAvCvDTO(Long.parseLong(aktivitetDTO.getVersjon()), false, AVTALT_DATO);

        var resultat = stillingFraNavController.oppdaterKanCvDeles(aktivitetData.getId(), delingAvCvDTO);
        var resultatJobbannonse = resultat.getStillingFraNavData();

        Assert.assertFalse(resultatJobbannonse.getCvKanDelesData().getKanDeles());
        Assert.assertNotNull(resultatJobbannonse.getCvKanDelesData().getEndretTidspunkt());
        Assert.assertEquals(InnsenderData.NAV, resultatJobbannonse.getCvKanDelesData().getEndretAvType());
        Assert.assertEquals(KJENT_SAKSBEHANDLER.get(), resultatJobbannonse.getCvKanDelesData().getEndretAv());
        Assert.assertEquals(AktivitetStatus.AVBRUTT, resultat.getStatus());
        Assert.assertEquals(AVTALT_DATO, resultatJobbannonse.getCvKanDelesData().avtaltDato);


    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterKanCvDeles_feilAktivitetstype_feiler() {
        var aktivitetData = aktivitetDAO.opprettNyAktivitet(nyMoteAktivitet().withAktorId(KJENT_AKTOR_ID.get()));
        var delecvDTO = new DelingAvCvDTO(aktivitetData.getVersjon(), true, AVTALT_DATO);
        stillingFraNavController.oppdaterKanCvDeles(aktivitetData.getId(), delecvDTO);
    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterKanCvDeles_feilVersjon_feiler() {
        var aktivitetData = aktivitetDAO.opprettNyAktivitet(nyMoteAktivitet().withAktorId(KJENT_AKTOR_ID.get()));
        var delecvDTO = new DelingAvCvDTO(new Random().nextLong(), true, AVTALT_DATO);
        stillingFraNavController.oppdaterKanCvDeles(aktivitetData.getId(), delecvDTO);
    }
}
