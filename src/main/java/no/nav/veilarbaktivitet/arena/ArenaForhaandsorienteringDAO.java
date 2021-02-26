package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArenaForhaandsorienteringDAO {

    private final Database database;

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

    public List<ArenaForhaandsorienteringData> hentForhaandsorienteringer(List<ArenaAktivitetDTO> aktiviteter) {
        List<String> aktivitetIder = aktiviteter.stream().map(ArenaAktivitetDTO::getId).collect(Collectors.toList());

        //language=SQL
        return database.query("SELECT * FROM ARENA_FORHAANDSORIENTERING A WHERE A.arenaaktivitet_id IN ?", ForhaandsorienteringDataRowMapper::mapForhaandsorientering, aktivitetIder);
    }
}
