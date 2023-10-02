package no.nav.veilarbaktivitet.avtalt_med_nav;

import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class ForhaandsorienteringDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final ForhaandsorienteringDAO fhoDAO = new ForhaandsorienteringDAO(database, database.getNamedJdbcTemplate());
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(new NamedParameterJdbcTemplate(jdbcTemplate));

    @AfterEach
    void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    void insertForAktivitet_oppdatererAlleFelter() {
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
    void insertForArenaAktivitet_oppdatererAlleFelter() {
        ArenaAktivitetDTO aktivitetData = new ArenaAktivitetDTO();
        ArenaId arenaId = new ArenaId("ARENATA123");
        aktivitetData.setId(arenaId.id());
        aktivitetData.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        String veileder = "V123";

        var fho = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tralala")
                .build();

        Forhaandsorientering fhoResultat = fhoDAO.insertForArenaAktivitet(fho, arenaId, AKTOR_ID, veileder, new Date(), Optional.empty());

        assertEquals(fho.getType(), fhoResultat.getType());
        assertEquals(fho.getTekst(), fhoResultat.getTekst());
        assertNull(fhoResultat.getAktivitetId());
        assertNull(fhoResultat.getAktivitetVersjon());
        assertEquals(aktivitetData.getId(), fhoResultat.getArenaAktivitetId());
        assertEquals(veileder, fhoResultat.getOpprettetAv());
        assertNull(fhoResultat.getLestDato());

    }

    @Test
    void skal_ikke_kunne_opprette_flere_fho_pa_arenaaktivitet() {
        ArenaAktivitetDTO aktivitetData = new ArenaAktivitetDTO();
        ArenaId arenaId = new ArenaId("ARENATA123");
        aktivitetData.setId(new ArenaId("ARENATA123").id());
        aktivitetData.setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET);
        String veileder = "V123";

        var fho = ForhaandsorienteringDTO.builder()
                .type(Type.SEND_FORHAANDSORIENTERING)
                .tekst("tralala")
                .build();

        fhoDAO.insertForArenaAktivitet(fho, arenaId, AKTOR_ID, veileder, new Date(), Optional.empty());
        assertThrows(ResponseStatusException.class, () -> {
            fhoDAO.insertForArenaAktivitet(fho, arenaId, AKTOR_ID, veileder, new Date(), Optional.empty());
        });
    }


    @Test
    void settVarselFerdig_varselErAlleredeStoppet_endrerIkkeStoppDato() {
        var avtaltDTO = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());

        var fho = fhoDAO.insert(avtaltDTO, new Random().nextLong(), AKTOR_ID, "V234", new Date());
        var fhoId = fho.getId();
        var stoppet = fhoDAO.settVarselFerdig(fhoId);

        fho = fhoDAO.getById(fhoId);
        var stoppetDato = fho.getVarselSkalStoppesDato();
        Assertions.assertTrue(stoppet);
        assertNotNull(stoppetDato);

        stoppet = fhoDAO.settVarselFerdig(fhoId);

        var fhoStoppetIgjen = fhoDAO.getById(fhoId);

        Assertions.assertFalse(stoppet);
        assertEquals(stoppetDato, fhoStoppetIgjen.getVarselSkalStoppesDato());
    }

    @Test
    void markerSomLest_setterRiktigeVerdier() {
        var avtaltDTO = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var lestDato = new Date();
        var fho = fhoDAO.insert(avtaltDTO, new Random().nextLong(), AKTOR_ID, "V234", new Date());
        var fhoId = fho.getId();

        fhoDAO.markerSomLest(fhoId, lestDato, avtaltDTO.getAktivitetVersjon());

        var nyFho = fhoDAO.getById(fhoId);

        assertEquals(lestDato, nyFho.getLestDato());
        assertEquals(lestDato, nyFho.getVarselSkalStoppesDato());
        assertEquals(String.valueOf(avtaltDTO.getAktivitetVersjon()), nyFho.getAktivitetVersjon());
        assertEquals(String.valueOf(avtaltDTO.getAktivitetVersjon()), nyFho.getAktivitetVersjon());

    }

    @Test
    void markerSomLest_forhaandsorienteringEksistererIkke_kasterException() {
        Date lestDato = new Date();
        Assertions.assertThrows(IllegalStateException.class, () ->
        fhoDAO.markerSomLest("Denne finnes ikke", lestDato, 1L));
    }

    @Test
    void getById_forhaandsorienteringEksistererIkke_returnererNull() {
        assertNull(fhoDAO.getById("Denne finnes ikke"));
    }

    @Test
    void getFhoForAktivitet_forhaandsorienteringEksistererIkke_returnererNull() {
        assertNull(fhoDAO.getFhoForAktivitet(5000L));
    }

    @Test
    void getById_flereIder_returnererAlle() {
        var avtalt1 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var avtalt2 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test2").build());

        var fho1 = fhoDAO.insert(avtalt1, new Random().nextLong(), AKTOR_ID, "V123", new Date());
        var fho2 = fhoDAO.insert(avtalt2, new Random().nextLong(), AKTOR_ID, "V123", new Date());

        var result = fhoDAO.getById(List.of(fho1.getId(), fho2.getId()));
        assertEquals(2, result.size());
    }

    @Test
    void getById_ingenIder_returnererTomListe() {
        var avtalt1 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
        var avtalt2 = new AvtaltMedNavDTO().setAktivitetVersjon(new Random().nextLong()).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test2").build());

         fhoDAO.insert(avtalt1, new Random().nextLong(), AKTOR_ID, "V123", new Date());
         fhoDAO.insert(avtalt2, new Random().nextLong(), AKTOR_ID, "V123", new Date());

        var result = fhoDAO.getById(Collections.emptyList());
        assertEquals(0, result.size());
    }

    @Test
    void getById_medStorListe_returnererAlleTreff() {
        List<String> forhaandsorienteringIder = LongStream.range(0, 1100).mapToObj(generatedId -> {
            var avtalt = new AvtaltMedNavDTO().setAktivitetVersjon(1).setForhaandsorientering(ForhaandsorienteringDTO.builder().type(Type.SEND_FORHAANDSORIENTERING).tekst("test").build());
            Forhaandsorientering forhaandsorientering = fhoDAO.insert(avtalt, generatedId, AKTOR_ID, "V123", new Date());
            return forhaandsorientering.getId();
        }).collect(Collectors.toList());

        var result = fhoDAO.getById(forhaandsorienteringIder);
        assertEquals(1100, result.size());
    }
}
