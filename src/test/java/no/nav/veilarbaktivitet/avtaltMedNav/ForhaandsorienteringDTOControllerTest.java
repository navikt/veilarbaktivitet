package no.nav.veilarbaktivitet.avtaltMedNav;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.types.identer.NavIdent;
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
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static java.lang.Long.parseLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ForhaandsorienteringDTOControllerTest {
    private String aktorid = "12345678";
    private String ident = "V12345678";
    @Mock
    private MetricService metricService;


    private JdbcTemplate jdbc = LocalH2Database.getDb();

    private AktivitetDAO aktivitetDAO = new AktivitetDAO(new Database(jdbc));
    private ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(new Database(jdbc));

    @Mock
    private AuthService authService;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();


    private AvtaltMedNavController avtaltMedNavController;

    @Before
    public void setup() {
        AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDao, meterRegistry);
        avtaltMedNavController = new AvtaltMedNavController(authService, avtaltMedNavService);

        DbTestUtils.cleanupTestDb(jdbc);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangOgInternBruker(any(), any());

        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);

        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(Person.aktorId(aktorid)));
    }

    @Test
    public void opprettFHO_opprettesPaaRiktigAktivitet() {
        String tekst = "fho tekst";
        Type type = Type.SEND_FORHAANDSORIENTERING;
        var aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(aktorid);
        aktivitetDAO.insertAktivitet(aktivitet);
        aktivitetDAO.insertAktivitet(aktivitet.withBeskrivelse("Beskrivelse"));
        var sisteAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());

        var avtaltDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(sisteAktivitet.getVersjon())
                .setForhaandsorientering(ForhaandsorienteringDTO.builder().tekst(tekst).type(type).build());

       // when(authService.sjekkTilgangOgInternBruker(ak))
        var opprettetAktivitet = avtaltMedNavController.opprettFHO(avtaltDTO, aktivitet.getId());
        var opprettetFHO = opprettetAktivitet.getForhaandsorientering();
        long forventetVersjon = sisteAktivitet.getVersjon()+1;

        Assert.assertEquals(tekst, opprettetFHO.getTekst());
        Assert.assertEquals(type, opprettetFHO.getType());
        Assert.assertNull(opprettetFHO.getLestDato());
        Assert.assertEquals(forventetVersjon, parseLong(opprettetAktivitet.getVersjon()));

    }

    @Test(expected = ResponseStatusException.class)
    public void skalSjekkeTilgangTilBruker() {
        AktivitetData aktivitetData = opprettAktivitet("0987654");

        avtaltMedNavController.opprettFHO(lagForhaandsorentering(aktivitetData), aktivitetData.getId());
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

        avtaltMedNavController.opprettFHO(lagForhaandsorentering(aktivitetData), aktivitetData.getId());
    }

    @Test(expected = ResponseStatusException.class)
    public void forhandsorenteringSkallIkkeVereNull() {
        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setAktivitetVersjon(aktivitetData.getVersjon());
        avtaltMedNavController.opprettFHO(fho, aktivitetData.getId());
    }

    @Test(expected = ResponseStatusException.class)
    public void versjonskonfliktSkalGiException() {
        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = lagForhaandsorentering(aktivitetData);
        fho.setAktivitetVersjon(fho.getAktivitetVersjon() -1);
        avtaltMedNavController.opprettFHO(fho, aktivitetData.getId());
    }

    @Test
    public void skalTaVarePaaForhaandsorienteringsTekst() {
        String tekst = "tekst";
        AktivitetData orginal = opprettAktivitet(aktorid);
        AvtaltMedNavDTO avtaltMedNavDTO = new AvtaltMedNavDTO();
        avtaltMedNavDTO.setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst(tekst).lestDato(null).build());
        avtaltMedNavDTO.setAktivitetVersjon(orginal.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.opprettFHO(avtaltMedNavDTO, orginal.getId());
        var forventetFHO = Forhaandsorientering.builder().tekst(tekst).type(Type.SEND_FORHAANDSORIENTERING).lestDato(null).build();
        AktivitetData forventet = orginal
                .toBuilder()
                .avtalt(true)
                .forhaandsorientering(forventetFHO)
                .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                //endres altid ved oppdatering
                .versjon(parseLong(markertSomAvtalt.getVersjon()))
                .endretDato(markertSomAvtalt.getEndretDato())
                .build();

        AktivitetDTO forventetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(forventet);

        assertEquals(markertSomAvtalt.getForhaandsorientering().getTekst(), forventetDTO.getForhaandsorientering().getTekst());

    }



    private AvtaltMedNavDTO lagForhaandsorentering(AktivitetData orginal) {
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setForhaandsorientering(ForhaandsorienteringDTO.builder().
                type(Type.SEND_FORHAANDSORIENTERING)
                .tekst(null)
                .lestDato(null)
                .build());
        fho.setAktivitetVersjon(orginal.getVersjon());

        return fho;
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
        DateTime start = DateTime.now();
        AktivitetData aktivitet = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build());
        fho.setAktivitetVersjon(aktivitet.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.opprettFHO(fho, aktivitet.getId());
        assertNull(markertSomAvtalt.getForhaandsorientering().getLestDato());

        LestDTO lestDTO = new LestDTO(parseLong(markertSomAvtalt.getId()), parseLong(markertSomAvtalt.getVersjon()));

        AktivitetDTO aktivitetDTO = avtaltMedNavController.lest(lestDTO);

        Assertions.assertThat(aktivitetDTO.getTransaksjonsType()).isEqualTo(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST);

        DateTime stopp = DateTime.now();
        DateTime lest = aktivitetDTO.getForhaandsorientering().getLestDato();

        assertNotNull(aktivitetDTO.getForhaandsorientering().getLestDato());

        assertTrue(start.isBefore(lest) || start.equals(lest));
        assertTrue(stopp.isAfter(lest) || stopp.equals(lest));
    }
}
