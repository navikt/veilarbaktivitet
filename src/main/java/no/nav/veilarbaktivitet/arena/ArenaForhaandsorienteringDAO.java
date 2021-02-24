package no.nav.veilarbaktivitet.arena;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ArenaForhaandsorienteringDAO {

    private final Database database;

    @Autowired
    public ArenaForhaandsorienteringDAO(Database database) {
        this.database = database;
    }

    @Transactional
    public void insertForhaandsorientering(String arenaaktivitetId, Person.AktorId aktorId, Forhaandsorientering forhaandsorientering) {
        String forhaandsorienteringType = null;
        String forhaandsorienteringTekst = null;
        Date fhoOpprettetDato = new Date();

        if(forhaandsorientering != null) {
            forhaandsorienteringType = forhaandsorientering.getType().name();
            forhaandsorienteringTekst = forhaandsorientering.getTekst();
        }

        //language=SQL
        database.update("INSERT INTO ARENA_FORHAANDSORIENTERING(arenaaktivitet_id, aktor_id, fho_type, fho_tekst, fho_opprettet_dato) " +
                        "VALUES (?,?,?,?,?)",
                arenaaktivitetId,
                aktorId,
                forhaandsorienteringType,
                forhaandsorienteringTekst,
                fhoOpprettetDato
        );

        log.info("opprettet {}", forhaandsorientering);
    }

    public ArenaForhaandsorienteringData hentForhaandsorientering(String arenaaktivitetId) {
        //language=SQL
        return database.queryForObject("SELECT * FROM ARENA_FORHAANDSORIENTERING A WHERE A.arenaaktivitet_id = ?",
                ForhaandsorienteringDataRowMapper::mapForhaandsorientering,
                arenaaktivitetId
        );
    }

    public List<ArenaForhaandsorienteringData> hentForhaandsorienteringer(List<ArenaAktivitetDTO> aktiviteter) {
        List<String> aktivitetIder = aktiviteter.stream().map(ArenaAktivitetDTO::getId).collect(Collectors.toList());

        //language=SQL
        return database.query("SELECT * FROM ARENA_FORHAANDSORIENTERING A WHERE A.arenaaktivitet_id IN ?", ForhaandsorienteringDataRowMapper::mapForhaandsorientering, aktivitetIder);
    }
}
