package no.nav.veilarbaktivitet.avtaltMedNav;

import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class ForhaandsorienteringDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final ForhaandsorienteringDAO fhoDAO = new ForhaandsorienteringDAO(database);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

    @AfterEach
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void insertForAktivitet_oppdatererAlleFelter() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet();
        String veileder = "V123";
        aktivitetDAO.insertAktivitet(aktivitetData);

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
        var stoppetDato = fho.getVarselFerdigDato();
        Assert.assertTrue(stoppet);
        Assert.assertNotNull(stoppetDato);

        stoppet = fhoDAO.settVarselFerdig(fhoId);

        var fhoStoppetIgjen = fhoDAO.getById(fhoId);

        Assert.assertFalse(stoppet);
        Assert.assertEquals(stoppetDato, fhoStoppetIgjen.getVarselFerdigDato());
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
        Assert.assertEquals(lestDato, nyFho.getVarselFerdigDato());
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
}
