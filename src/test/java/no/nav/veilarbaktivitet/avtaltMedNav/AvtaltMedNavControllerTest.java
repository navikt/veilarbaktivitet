package no.nav.veilarbaktivitet.avtaltMedNav;


import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.MetricService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;


@RunWith(MockitoJUnitRunner.class)
public class AvtaltMedNavControllerTest {
    private String aktorid = "12345678";

    @Mock
    private MetricService metricService;


    private JdbcTemplate jdbc = LocalH2Database.getDb();

    private AktivitetDAO aktivitetDAO = new AktivitetDAO(new Database(jdbc));

    @Mock
    private AuthService authService;

    private AvtaltMedNavController avtaltMedNavController;

    @Before
    public void setup() {
        AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO);
        avtaltMedNavController = new AvtaltMedNavController(authService, avtaltMedNavService);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbc);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangOgInternBruker(any(), any());

        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);

    }

    @Test(expected = ResponseStatusException.class)
    public void skalSjekkeTilgangTilBruker() {
        AktivitetData aktivitetData = opprettAktivitet("0987654");

        avtaltMedNavController.markerSomAvtaltMedNav(lagForhaandsorentering(aktivitetData), aktivitetData.getId());
    }

    @Test(expected = ResponseStatusException.class)
    public void skalSjekkeKVP() {
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nySokeAvtaleAktivitet()
                .toBuilder()
                .aktorId(aktorid)
                .versjon(aktivitetDAO.getNextUniqueAktivitetId())
                .kontorsperreEnhetId("1234")
                .build();

        aktivitetDAO.insertAktivitet(aktivitetData);

        avtaltMedNavController.markerSomAvtaltMedNav(lagForhaandsorentering(aktivitetData), aktivitetData.getId());
    }

    @Test(expected = ResponseStatusException.class)
    public void forhandsorenteringSkallIkkeVereNull() {
        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNav avtaltMedNav = new AvtaltMedNav();
        avtaltMedNav.setAktivitetVersjon(aktivitetData.getVersjon());
        avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, aktivitetData.getId());
    }

    @Test(expected = ResponseStatusException.class)
    public void versjonskonfliktSkalGiException() {
        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNav avtaltMedNav = lagForhaandsorentering(aktivitetData);
        avtaltMedNav.setAktivitetVersjon(avtaltMedNav.getAktivitetVersjon() -1);
        avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, aktivitetData.getId());
    }

    @Test
    public void skalTaVarePaaForhaandsOretneringsTekst() {
        AktivitetData orginal = opprettAktivitet(aktorid);
        AvtaltMedNav avtaltMedNav = new AvtaltMedNav();
        avtaltMedNav.setForhaandsorientering(new Forhaandsorientering(Forhaandsorientering.Type.SEND_FORHAANDSORIENTERING, "kake", null));
        avtaltMedNav.setAktivitetVersjon(orginal.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, orginal.getId());

        AktivitetData forventet = orginal
                .toBuilder()
                .avtalt(true)
                .forhaandsorientering(avtaltMedNav.getForhaandsorientering())
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                //endres altid ved oppdatering
                .versjon(Long.parseLong(markertSomAvtalt.getVersjon()))
                .endretDato(markertSomAvtalt.getEndretDato())
                .build();

        AktivitetDTO forventetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(forventet);

        assertEquals(markertSomAvtalt, forventetDTO);

    }

    @Test
    public void markerSomAvtaltMedNav_skalVirkeForAlleAktivitetTyper() {
        Arrays.stream(AktivitetTypeData.values())
                .map(AktivitetDataTestBuilder::nyAktivitet)
                .map(a -> a.toBuilder().id(aktivitetDAO.getNextUniqueAktivitetId()).aktorId(aktorid).build())
                .forEach(aktivitetDAO::insertAktivitet);

        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorid));

        for (AktivitetData orginal :
                aktivitetData) {

            AvtaltMedNav avtaltMedNav = lagForhaandsorentering(orginal);
            AktivitetDTO markertSomAvtalt = avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, orginal.getId());

            AktivitetData forventet = orginal
                    .toBuilder()
                    .avtalt(true)
                    .forhaandsorientering(avtaltMedNav.getForhaandsorientering())
                    .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                    //endres altid ved oppdatering
                    .versjon(Long.parseLong(markertSomAvtalt.getVersjon()))
                    .endretDato(markertSomAvtalt.getEndretDato())
                    .build();

            AktivitetDTO forventetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(forventet);

            assertEquals(markertSomAvtalt, forventetDTO);
        }
    }

    private AvtaltMedNav lagForhaandsorentering(AktivitetData orginal) {
        AvtaltMedNav avtaltMedNav = new AvtaltMedNav();
        avtaltMedNav.setForhaandsorientering(new Forhaandsorientering(Forhaandsorientering.Type.IKKE_SEND_FORHAANDSORIENTERING, null, null));
        avtaltMedNav.setAktivitetVersjon(orginal.getVersjon());
        return avtaltMedNav;
    }

    private AktivitetData opprettAktivitet(String aktorid) {
        AktivitetData aktivitetData = AktivitetDataTestBuilder.nySokeAvtaleAktivitet()
                .toBuilder()
                .aktorId(aktorid)
                .versjon(aktivitetDAO.getNextUniqueAktivitetId())
                .build();

        aktivitetDAO.insertAktivitet(aktivitetData);
        return aktivitetDAO.hentAktivitet(aktivitetData.getId());
    }

    @Test
    public void markerForhaandsorienteringSomLest_skalVirke() {
        Date start = new Date();
        AktivitetData orginal = opprettAktivitet(aktorid);
        AvtaltMedNav avtaltMedNav = new AvtaltMedNav();
        avtaltMedNav.setForhaandsorientering(new Forhaandsorientering(Forhaandsorientering.Type.SEND_FORHAANDSORIENTERING, "kake", null));
        avtaltMedNav.setAktivitetVersjon(orginal.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.markerSomAvtaltMedNav(avtaltMedNav, orginal.getId());
        assertNull(markertSomAvtalt.getForhaandsorientering().getLest());

        LestDTO lestDTO = new LestDTO(Long.parseLong(markertSomAvtalt.getId()), Long.parseLong(markertSomAvtalt.getVersjon()));

        AktivitetDTO aktivitetDTO = avtaltMedNavController.lest(lestDTO);

        Assertions.assertThat(aktivitetDTO.getTransaksjonsType()).isEqualTo(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST);

        Date stopp = new Date();
        Date lest = aktivitetDTO.getForhaandsorientering().getLest();

        assertNotNull(aktivitetDTO.getForhaandsorientering().getLest());

        assertTrue(start.before(lest) || start.equals(lest));
        assertTrue(stopp.after(lest) || stopp.equals(lest));
    }
}
