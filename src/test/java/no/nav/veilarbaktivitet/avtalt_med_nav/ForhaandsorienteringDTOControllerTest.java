package no.nav.veilarbaktivitet.avtalt_med_nav;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ForhaandsorienteringDTOControllerTest {
    private final String aktorid = "12345678";
    private final String ident = "V12345678";
    @Mock
    private MetricService metricService;


    private final JdbcTemplate jdbc = LocalH2Database.getDb();

    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(new Database(jdbc), new NamedParameterJdbcTemplate(jdbc));
    private final ForhaandsorienteringDAO fhoDao = new ForhaandsorienteringDAO(new Database(jdbc), new Database(jdbc).getNamedJdbcTemplate());

    @Mock
    private AuthService authService;

    @Mock
    private BrukernotifikasjonService brukernotifikasjonService;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();


    private AvtaltMedNavController avtaltMedNavController;

    @BeforeEach
    void setup() {
        AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDao, meterRegistry, brukernotifikasjonService);
        avtaltMedNavController = new AvtaltMedNavController(authService, avtaltMedNavService);

        DbTestUtils.cleanupTestDb(jdbc);
    }

    @Test
    void opprettFHO_opprettesPaaRiktigAktivitet() {
        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);
        when(brukernotifikasjonService.kanVarsles(Person.aktorId(aktorid))).thenReturn(true);
        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));

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
        long forventetVersjon = sisteAktivitet.getVersjon() + 1;

        Assertions.assertEquals(tekst, opprettetFHO.getTekst());
        Assertions.assertEquals(type, opprettetFHO.getType());
        Assertions.assertNull(opprettetFHO.getLestDato());
        Assertions.assertEquals(forventetVersjon, parseLong(opprettetAktivitet.getVersjon()));

    }

    @Test
    void skalSjekkeTilgangTilBruker() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangOgInternBruker(any(), any());

        AktivitetData aktivitetData = opprettAktivitet("0987654");

        Assertions.assertThrows(ResponseStatusException.class, () ->
                avtaltMedNavController.opprettFHO(lagForhaandsorentering(aktivitetData), aktivitetData.getId()));
    }

    @Test
    void skalSjekkeKVP() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(authService)
                .sjekkTilgangOgInternBruker(any(), any());

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nySokeAvtaleAktivitet()
                .toBuilder()
                .aktorId(aktorid)
                .versjon(aktivitetDAO.nesteVersjon())
                .kontorsperreEnhetId("1234")
                .build();

        final AktivitetData aktivitetDataMedAktivitet = aktivitetDAO.opprettNyAktivitet(aktivitetData);

        AvtaltMedNavDTO avtaltMedNavDTO = lagForhaandsorentering(aktivitetDataMedAktivitet);
        Long id = aktivitetDataMedAktivitet.getId();
        Assertions.assertThrows(ResponseStatusException.class, () ->
                avtaltMedNavController.opprettFHO(avtaltMedNavDTO, id));
    }

    @Test
    void forhandsorenteringSkallIkkeVereNull() {

        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setAktivitetVersjon(aktivitetData.getVersjon());

        Long id = aktivitetData.getId();
        Assertions.assertThrows(ResponseStatusException.class, () ->
                avtaltMedNavController.opprettFHO(fho, id));
    }

    @Test
    void versjonskonfliktSkalGiException() {
        AktivitetData aktivitetData = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = lagForhaandsorentering(aktivitetData);
        fho.setAktivitetVersjon(fho.getAktivitetVersjon() - 1);
        Long id = aktivitetData.getId();
        Assertions.assertThrows(ResponseStatusException.class, () ->
                avtaltMedNavController.opprettFHO(fho, id));
    }

    @Test
    void skalTaVarePaaForhaandsorienteringsTekst() {
        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);
        when(brukernotifikasjonService.kanVarsles(Person.aktorId(aktorid))).thenReturn(true);
        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));
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
    void markerForhaandsorienteringSomLest_skalVirke() {
        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);
        doReturn(Optional.of(Person.navIdent(ident)))
                .when(authService).getLoggedInnUser();
        when(brukernotifikasjonService.kanVarsles(Person.aktorId(aktorid))).thenReturn(true);
        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));

        Date start = new Date();
        AktivitetData aktivitet = opprettAktivitet(aktorid);
        AvtaltMedNavDTO fho = new AvtaltMedNavDTO();
        fho.setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("kake").build());
        fho.setAktivitetVersjon(aktivitet.getVersjon());

        AktivitetDTO markertSomAvtalt = avtaltMedNavController.opprettFHO(fho, aktivitet.getId());
        assertNull(markertSomAvtalt.getForhaandsorientering().getLestDato());

        LestDTO lestDTO = new LestDTO(parseLong(markertSomAvtalt.getId()), parseLong(markertSomAvtalt.getVersjon()));

        AktivitetDTO aktivitetDTO = avtaltMedNavController.lest(lestDTO);

        assertThat(aktivitetDTO.getTransaksjonsType()).isEqualTo(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST);

        Date stopp = new Date();
        Date lest = aktivitetDTO.getForhaandsorientering().getLestDato();

        assertNotNull(aktivitetDTO.getForhaandsorientering().getLestDato());

        assertTrue(start.before(lest) || start.equals(lest));
        assertTrue(stopp.after(lest) || stopp.equals(lest));
    }

    @Test
    void markerSomAvtaltMedNav_skalVirkeForAlleAktivitetTyper() {

        doNothing()
                .when(authService)
                .sjekkTilgangOgInternBruker(aktorid, null);
        when(brukernotifikasjonService.kanVarsles(Person.aktorId(aktorid))).thenReturn(true);
        when(authService.getInnloggetVeilederIdent()).thenReturn(new NavIdent(ident));
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
