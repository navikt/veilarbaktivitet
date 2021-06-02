package no.nav.veilarbaktivitet.avtaltMedNav;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.MetricService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AvtaltMedNavServiceTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");
    private static final NavIdent veilederIdent = NavIdent.of("V123");
    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final ForhaandsorienteringDAO fhoDAO = new ForhaandsorienteringDAO(database);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

    private MetricService metricService = mock(MetricService.class);

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDAO, meterRegistry);
    final String defaultTekst = "tekst";
    final Type defaultType = Type.SEND_FORHAANDSORIENTERING;

    @Test
    public void opprettFHO_oppdatererInterneFHOVerdier() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        opprettAktivitetMedFHO(aktivitetData);
        var fho = avtaltMedNavService.hentFhoForAktivitet(aktivitetData.getId());

        Assert.assertEquals(aktivitetData.getId().toString(), fho.getAktivitetId());
        Assert.assertEquals(aktivitetData.getVersjon().toString(), fho.getAktivitetVersjon());
        Assert.assertEquals(aktivitetData.getAktorId(), fho.getAktorId().get());
        Assert.assertEquals(veilederIdent.toString(), fho.getOpprettetAv());
        assertNull(fho.getLestDato());

    }

    @Test
    public void opprettFHO_oppdatererAktivitetDTO() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        var aktivitetDTO = opprettAktivitetMedFHO(aktivitetData);
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();
        var nyAktivitetMedFHO = aktivitetDAO.hentAktivitet(aktivitetData.getId());

        Assert.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assert.assertEquals(defaultType, aktivitetDTOFHO.getType());
        Assert.assertNotNull(aktivitetDTOFHO.getId());
        Assert.assertNull(aktivitetDTOFHO.getLestDato());
        Assert.assertEquals(AktivitetTransaksjonsType.AVTALT, nyAktivitetMedFHO.getTransaksjonsType());

    }

    @Test
    public void opprettFHO_medTomTekst_setterTekstenTilNull() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst("").build();

        var aktivitetDTO = opprettAktivitetMedFHO(aktivitetData, fhoDTO);
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();

        Assert.assertEquals(null, aktivitetDTOFHO.getTekst());

    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void opprettFHO_maaHaMatchendeAktivitet() {
        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst(defaultTekst).lestDato(null).build();

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(fhoDTO)
                .setAktivitetVersjon(5L);
        avtaltMedNavService.opprettFHO(avtaltDTO, 1, AKTOR_ID, veilederIdent);

    }

    @Test
    public void markerSomLest_oppdatererAktivitetDTO() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        opprettAktivitetMedFHO(aktivitetData);

        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(aktivitetData.getId());
        var aktivitetLest = avtaltMedNavService.markerSomLest(aktivitetDTOFHO, AKTOR_ID);
        var nyAktivitetMedFHO = aktivitetDAO.hentAktivitet(aktivitetData.getId());

        Assert.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assert.assertEquals(defaultType, aktivitetDTOFHO.getType());
        Assert.assertNotNull(aktivitetLest.getForhaandsorientering().getLestDato());
        Assert.assertEquals(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST, nyAktivitetMedFHO.getTransaksjonsType());

    }

    @Test
    public void stoppVarselHvisAktiv_stopperAktivtVarsel() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());
        var opprettetAktivitetMedFHO = opprettAktivitetMedFHO(aktivitetData);
        boolean varselStoppet = avtaltMedNavService.stoppVarselHvisAktiv(opprettetAktivitetMedFHO.getForhaandsorientering().getId());
        var nyFHO = avtaltMedNavService.hentFHO(opprettetAktivitetMedFHO.getForhaandsorientering().getId());

        Assert.assertTrue(varselStoppet);
        Assert.assertNotNull(nyFHO.getVarselStoppetDato());

    }

    @Test
    public void stoppVarselHvisAktiv_ForhåndsorienteringsIdErNULL_returnererFalse() {
        var varselStoppet = avtaltMedNavService.stoppVarselHvisAktiv(null);

        Assert.assertFalse(varselStoppet);
    }

    private AktivitetDTO opprettAktivitetMedFHO(AktivitetData aktivitetData) {
        aktivitetDAO.insertAktivitet(aktivitetData);

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst(defaultTekst).lestDato(null).build();

        return opprettAktivitetMedFHO(aktivitetData, fhoDTO);

    }

    private AktivitetDTO opprettAktivitetMedFHO(AktivitetData aktivitetData, ForhaandsorienteringDTO forhaandsorienteringDTO) {
        aktivitetDAO.insertAktivitet(aktivitetData);

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(forhaandsorienteringDTO)
                .setAktivitetVersjon(aktivitetData.getVersjon());

        return avtaltMedNavService.opprettFHO(avtaltDTO, aktivitetData.getId(), AKTOR_ID, veilederIdent);
    }

}
