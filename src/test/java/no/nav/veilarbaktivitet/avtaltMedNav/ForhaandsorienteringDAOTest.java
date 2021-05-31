package no.nav.veilarbaktivitet.avtaltMedNav;

import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class ForhaandsorienteringDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final ForhaandsorienteringDAO fhoDAO = new ForhaandsorienteringDAO(database);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);

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
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet();
        String veileder = "V123";
        aktivitetDAO.insertAktivitet(aktivitetData);

        var fho = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tralala")
                .build();

        AvtaltMedNavDTO fhoDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(fho)
                .setAktivitetVersjon(aktivitetData.getVersjon());

        UUID id = fhoDAO.insertForArenaAktivitet(fhoDTO,aktivitetData.getId(), AKTOR_ID, veileder);

        var fhoResultat = fhoDAO.getById(id.toString());

        assertEquals(fho.getType(), fhoResultat.getType());
        assertEquals(fhoDTO.getForhaandsorientering().getTekst(), fhoResultat.getTekst());
        assertEquals(id.toString(), fhoResultat.getId());
        assertNull(fhoResultat.getAktivitetId());
        assertNull(fhoResultat.getAktivitetVersjon());
        assertEquals(aktivitetData.getId().toString(), fhoResultat.getArenaaktivitetId());
        assertEquals(veileder, fhoResultat.getOpprettetAv());
        assertNull(fhoResultat.getLestDato());

    }


}
