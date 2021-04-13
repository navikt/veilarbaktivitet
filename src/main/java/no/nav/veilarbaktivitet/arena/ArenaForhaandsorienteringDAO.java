package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArenaForhaandsorienteringDAO {

    private final Database database;

    void insertForhaandsorientering(String arenaaktivitetId, Person.AktorId aktorId, Forhaandsorientering forhaandsorientering, String opprettetAv) {
        Date fhoOpprettetDato = new Date();

        //language=SQL
        database.update("INSERT INTO ARENA_FORHAANDSORIENTERING(arenaaktivitet_id, aktor_id, fho_type, fho_tekst, fho_opprettet_dato, opprettet_av_ident) " +
                        "VALUES (?,?,?,?,?,?)",
                arenaaktivitetId,
                aktorId.get(),
                forhaandsorientering.getType().name(),
                forhaandsorientering.getTekst(),
                fhoOpprettetDato,
                opprettetAv
        );

        log.info("opprettet {}", forhaandsorientering);
    }

    List<ArenaForhaandsorienteringData> hentForhaandsorienteringer(Person.AktorId aktorId) {
        //language=SQL
        return database.query("SELECT * FROM ARENA_FORHAANDSORIENTERING WHERE aktor_id = ?", ArenaForhaandsorienteringDAO::mapForhaandsorientering, aktorId.get());
    }

    private static ArenaForhaandsorienteringData mapForhaandsorientering(ResultSet rs) throws SQLException {
        return ArenaForhaandsorienteringData
                .builder()
                .arenaktivitetId(rs.getString("arenaaktivitet_id"))
                .aktorId(rs.getString("aktor_id"))
                .forhaandsorientering(Forhaandsorientering
                        .builder()
                        .type(EnumUtils.valueOf(Forhaandsorientering.Type.class, rs.getString("fho_type")))
                        .tekst(rs.getString("fho_tekst"))
                        .lest(Database.hentDato(rs, "lest"))
                        .build())
                .opprettetDato(Database.hentDato(rs, "fho_opprettet_dato"))
                .build();
    }

    public boolean markerSomLest(Person.AktorId aktorId, String aktivitetId) {
        //language=SQL
        return 1L == database
                .update("update ARENA_FORHAANDSORIENTERING set lest = current_timestamp where AKTOR_ID = ? and ARENAAKTIVITET_ID = ? and lest is null", aktorId.get(), aktivitetId) ;

    }
}
