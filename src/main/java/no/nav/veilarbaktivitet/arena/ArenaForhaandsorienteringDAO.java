package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArenaForhaandsorienteringDAO {

    private final Database database;

    void insertForhaandsorientering(String arenaaktivitetId, Person.AktorId aktorId, Forhaandsorientering forhaandsorientering) {
        Date fhoOpprettetDato = new Date();

        //language=SQL
        database.update("INSERT INTO ARENA_FORHAANDSORIENTERING(arenaaktivitet_id, aktor_id, fho_type, fho_tekst, fho_opprettet_dato) " +
                        "VALUES (?,?,?,?,?)",
                arenaaktivitetId,
                aktorId.get(),
                forhaandsorientering.getType().name(),
                forhaandsorientering.getTekst(),
                fhoOpprettetDato
        );

        log.info("opprettet {}", forhaandsorientering);
    }

    List<ArenaForhaandsorienteringData> hentForhaandsorienteringer(List<ArenaAktivitetDTO> aktiviteter) {
        if(aktiviteter.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> aktivitetIder = aktiviteter.stream().map(ArenaAktivitetDTO::getId).collect(Collectors.toList());

        //language=SQL
        return database.query("SELECT * FROM ARENA_FORHAANDSORIENTERING A WHERE A.arenaaktivitet_id IN (?)", ArenaForhaandsorienteringDAO::mapForhaandsorientering, aktivitetIder);
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
                        .build())
                .opprettetDato(Database.hentDato(rs, "fho_opprettet_dato"))
                .build();
    }
}
