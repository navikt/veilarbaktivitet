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
import org.junit.Assert;
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

        doReturn(Optional.of(Person.navIdent(ident)))
                .when(authService).getLoggedInnUser();

        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));
    }

    @Test
    public void opprettFHO_opprettesPaaRiktigAktivitet() {
        String tekst = "fho tekst";
        Type type = Type.SEND_FORHAANDSORIENTERING;
        var aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(aktorid);
        aktivitet = aktivitetDAO.opprettNyAktivitet(aktivitet);
        aktivitetDAO.oppdaterAktivitet(aktivitet.withBeskrivelse("Beskrivelse"));
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
                .versjon(aktivitetDAO.nesteVersjon())
                .kontorsperreEnhetId("1234")
                .build();

        aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitetData);

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
                .lagtInnAv(InnsenderData.NAV)
                .endretAv(ident)
                .build();

        AktivitetDTO forventetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(forventet, false);

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
                .versjon(aktivitetDAO.nesteVersjon())
                .build();

        return aktivitetDAO.opprettNyAktivitet(aktivitetData);
    }

    @Test
    public void markerForhaandsorienteringSomLest_skalVirke() {
        Date start = new Date();
        AktivitetData aktivitet = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build());
        fho.setAktivitetVersjon(aktivitet.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.opprettFHO(fho, aktivitet.getId());
        assertNull(markertSomAvtalt.getForhaandsorientering().getLestDato());

        LestDTO lestDTO = new LestDTO(parseLong(markertSomAvtalt.getId()), parseLong(markertSomAvtalt.getVersjon()));

        AktivitetDTO aktivitetDTO = avtaltMedNavController.lest(lestDTO);

        Assertions.assertThat(aktivitetDTO.getTransaksjonsType()).isEqualTo(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST);

        Date stopp = new Date();
        Date lest = aktivitetDTO.getForhaandsorientering().getLestDato();

        assertNotNull(aktivitetDTO.getForhaandsorientering().getLestDato());

        assertTrue(start.before(lest) || start.equals(lest));
        assertTrue(stopp.after(lest) || stopp.equals(lest));
    }

    @Test
    public void markerSomAvtaltMedNav_skalVirkeForAlleAktivitetTyper() {
        Arrays.stream(AktivitetTypeData.values())
                .map(AktivitetDataTestBuilder::nyAktivitet)
                .map(a -> a.toBuilder().aktorId(aktorid).build())
                .forEach(aktivitetDAO::opprettNyAktivitet);

        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktiviteterForAktorId(Person.aktorId(aktorid));

        for (AktivitetData orginal :
                aktivitetData) {

            var fhoSomSkalLages = lagForhaandsorentering(orginal);
            AktivitetDTO resultatDTO = avtaltMedNavController.opprettFHO(fhoSomSkalLages, orginal.getId());

            var forventetFHO = Forhaandsorientering.builder()
                    .type(fhoSomSkalLages.getForhaandsorientering().getType())
                    .tekst(fhoSomSkalLages.getForhaandsorientering().getTekst())
                    .id(resultatDTO.getForhaandsorientering().getId())
                    .build();

            resultatDTO.setStillingFraNavData(null);
            AktivitetData forventet = orginal
                    .toBuilder()
                    .avtalt(true)
                    .forhaandsorientering(forventetFHO)
                    .transaksjonsType(AktivitetTransaksjonsType.AVTALT)
                    .stillingFraNavData(null)
                    //endres altid ved oppdatering
                    .versjon(Long.parseLong(resultatDTO.getVersjon()))
                    .lagtInnAv(InnsenderData.NAV)
                    .endretAv(ident)
                    .endretDato(resultatDTO.getEndretDato())
                    .build();

            AktivitetDTO forventetDTO = AktivitetDTOMapper.mapTilAktivitetDTO(forventet, false);

            assertEquals(forventetDTO, resultatDTO);
        }
    }
}
