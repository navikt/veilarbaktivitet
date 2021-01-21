package no.nav.veilarbaktivitet.avtaltMedNav;


import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.FunksjonelleMetrikker;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class AvtaltMedNavControllerTest {
    private String aktorid = "12345678";

    @Mock
    private FunksjonelleMetrikker funksjonelleMetrikker;

    @Mock
    private AktorregisterClient aktorregisterClient;
    @Mock
    private Pep veilarbPep;

    private JdbcTemplate jdbc = LocalH2Database.getDb();

    private AktivitetDAO aktivitetDAO = new AktivitetDAO(new Database(jdbc));

    @InjectMocks
    private AuthService authService;

    private AvtaltMedNavController avtaltMedNavController;

    @Before
    public void setup() {
        avtaltMedNavController = new AvtaltMedNavController(funksjonelleMetrikker, aktivitetDAO, authService);
    }

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbc);

    }

    @Test
    public void markerSomAvtaltMedNav_skalVirkeForAlleAktivitetTyper() {
        Arrays.stream(AktivitetTypeData.values())
                .map(AktivitetDataTestBuilder::nyAktivitet)
                .map(a -> a.toBuilder().id( aktivitetDAO.getNextUniqueAktivitetId()).build())
                .forEach(aktivitetDAO::insertAktivitet);

        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorid));

        aktivitetData
                .stream()
                .map(aktivitet -> avtaltMedNavController.markerSomAvtaltMedNav(new AvtaltMedNav(), aktivitet.getId()))
                .map(AktivitetDataMapper::mapTilAktivitetData);

    }

    private AktivitetDTO markerAktivtetSomAvtalt(AktivitetData aktivitet) {
        AvtaltMedNav avtaltMedNav = new AvtaltMedNav();
        avtaltMedNav.setAktivitetVersjon(aktivitet.getVersjon());
        avtaltMedNav.setForhaandsorientering(new Forhaandsorientering(Forhaandsorientering.Type.SEND_FORHAANDSORIENTERING, "kake"));
        return avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, aktivitet.getId());
    }


}
