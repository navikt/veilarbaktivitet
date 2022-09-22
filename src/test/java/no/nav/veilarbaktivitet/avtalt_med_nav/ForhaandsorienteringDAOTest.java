package no.nav.veilarbaktivitet.avtalt_med_nav;

import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.*;

public class ForhaandsorienteringDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final ForhaandsorienteringDAO fhoDAO = new ForhaandsorienteringDAO(database);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database, new NamedParameterJdbcTemplate(jdbcTemplate));

    @After
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void insertForAktivitet_oppdatererAlleFelter() {
        var aktivitetData = aktivitetDAO.opprettNyAktivitet(AktivitetDataTestBuilder.nyEgenaktivitet());
        String veileder = "V123";

        var fho = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tekst").lestDato(null).build();
        AvtaltMedNavDTO fhoDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(fho)
                .setAktivitetVersjon(aktivitetData.getVersjon());

        var fhoResultat = fhoDAO.insert(fhoDTO,aktivitetData.getId(), AKTOR_ID, veileder, new Date());

        assertEquals(Type.SEND_FORHAANDSORIENTERING, fhoResultat.getType());
        assertEquals(fhoDTO.getForhaandsorientering().getTekst(), fhoResultat.getTekst());
        assertEquals(aktivitetData.getId().toString(), fhoResultat.getAktivitetId());
        assertEquals(String.valueOf(fhoDTO.getAktivitetVersjon()), fhoResultat.getAktivitetVersjon());
        assertEquals(veileder, fhoResultat.getOpprettetAv());
        assertNotNull(fhoResultat.getOpprettetDato());
        assertNull(fhoResultat.getLestDato());

    }

    @Test
    public void insertForArenaAktivitet_oppdatererAlleFelter() {
        ArenaAktivitetDTO aktivitetData = new ArenaAktivitetDTO();
        aktivitetData.setId("arenaId");
        aktivitetData.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        String veileder = "V123";

        var fho = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tralala")
                .build();

        Forhaandsorientering fhoResultat = fhoDAO.insertForArenaAktivitet(fho, aktivitetData.getId(), AKTOR_ID, veileder, new Date());

        assertEquals(fho.getType(), fhoResultat.getType());
        assertEquals(fho.getTekst(), fhoResultat.getTekst());
        assertNull(fhoResultat.getAktivitetId());
        assertNull(fhoResultat.getAktivitetVersjon());
        assertEquals(aktivitetData.getId(), fhoResultat.getArenaAktivitetId());
        assertEquals(veileder, fhoResultat.getOpprettetAv());
        assertNull(fhoResultat.getLestDato());

    }


    @Test
    public void settVarselFerdig_varselErAlleredeStoppet_endrerIkkeStoppDato() {
        var avtaltDTO = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());

        var fho = fhoDAO.insert(avtaltDTO, new Random().nextLong(), AKTOR_ID, "V234", new Date());
        var fhoId = fho.getId();
        var stoppet = fhoDAO.settVarselFerdig(fhoId);

        fho = fhoDAO.getById(fhoId);
        var stoppetDato = fho.getVarselSkalStoppesDato();
        Assert.assertTrue(stoppet);
        Assert.assertNotNull(stoppetDato);

        stoppet = fhoDAO.settVarselFerdig(fhoId);

        var fhoStoppetIgjen = fhoDAO.getById(fhoId);

        Assert.assertFalse(stoppet);
        Assert.assertEquals(stoppetDato, fhoStoppetIgjen.getVarselSkalStoppesDato());
    }

    @Test
    public void markerSomLest_setterRiktigeVerdier() {
        var avtaltDTO = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var lestDato = new Date();
        var fho = fhoDAO.insert(avtaltDTO, new Random().nextLong(), AKTOR_ID, "V234", new Date());
        var fhoId = fho.getId();

        fhoDAO.markerSomLest(fhoId, lestDato, avtaltDTO.getAktivitetVersjon());

        var nyFho = fhoDAO.getById(fhoId);

        Assert.assertEquals(lestDato, nyFho.getLestDato());
        Assert.assertEquals(lestDato, nyFho.getVarselSkalStoppesDato());
        Assert.assertEquals(String.valueOf(avtaltDTO.getAktivitetVersjon()), nyFho.getAktivitetVersjon());
        Assert.assertEquals(String.valueOf(avtaltDTO.getAktivitetVersjon()), nyFho.getAktivitetVersjon());

    }

    @Test(expected = IllegalStateException.class)
    public void markerSomLest_forhaandsorienteringEksistererIkke_kasterException() {
        fhoDAO.markerSomLest("Denne finnes ikke", new Date(), 1L);
    }

    @Test
    public void getById_forhaandsorienteringEksistererIkke_returnererNull() {
        Assert.assertNull(fhoDAO.getById("Denne finnes ikke"));
    }

    @Test
    public void getFhoForAktivitet_forhaandsorienteringEksistererIkke_returnererNull() {
        Assert.assertNull(fhoDAO.getFhoForAktivitet(5000L));
    }

    @Test
    public void getById_flereIder_returnererAlle() {
        var avtalt1 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var avtalt2 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test2").build());

        var fho1 = fhoDAO.insert(avtalt1, new Random().nextLong(), AKTOR_ID, "V123", new Date());
        var fho2 = fhoDAO.insert(avtalt2, new Random().nextLong(), AKTOR_ID, "V123", new Date());

        var result = fhoDAO.getById(List.of(fho1.getId(), fho2.getId()));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void getById_ingenIder_returnererTomListe() {
        var avtalt1 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var avtalt2 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test2").build());

         fhoDAO.insert(avtalt1, new Random().nextLong(), AKTOR_ID, "V123", new Date());
         fhoDAO.insert(avtalt2, new Random().nextLong(), AKTOR_ID, "V123", new Date());

        var result = fhoDAO.getById(Collections.emptyList());
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void getById_medStorListe_returnererAlleTreff() {
        List<String> forhaandsorienteringIder = LongStream.range(0, 1100).mapToObj(generatedId -> {
            var avtalt = new AvtaltMedNavDTO().setAktivitetVersjon(1).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
            Forhaandsorientering forhaandsorientering = fhoDAO.insert(avtalt, generatedId, AKTOR_ID, "V123", new Date());
            return forhaandsorientering.getId();
        }).collect(Collectors.toList());

        var result = fhoDAO.getById(forhaandsorienteringIder);
        Assert.assertEquals(1100, result.size());
    }
}
