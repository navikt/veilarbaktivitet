package no.nav.veilarbaktivitet.avtaltMedNav;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
public class ForhaandsorienteringDAO {
    private final Database database;
    private final static String selectAktivitet = "SELECT ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO " +
            "FROM FORHAANDSORIENTERING ";

    private static final Logger LOG = getLogger(ForhaandsorienteringDAO.class);

    public ForhaandsorienteringDAO(Database database) {
        this.database = database;
    }

    public Forhaandsorientering insert(AvtaltMedNavDTO fhoData, long aktivitetId, Person.AktorId aktorId, String opprettetAv, Date opprettetDato) {
        var id = UUID.randomUUID();
        var fho = fhoData.getForhaandsorientering();
        database.update("INSERT INTO FORHAANDSORIENTERING(ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV, LEST_DATO)" +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                id.toString(),
                aktorId.get(),
                aktivitetId,
                fhoData.getAktivitetVersjon(),
                null,
                fho.getType().name(),
                fho.getTekst(),
                opprettetDato,
                opprettetAv,
                null
        );

        LOG.info("opprettet forhåndsorientering: {} med id: {}", fhoData, id);

        return getById(id.toString());
    }

    public UUID insertForArenaAktivitet(AvtaltMedNavDTO fhoData, long aktivitetId, Person.AktorId aktorId, String opprettetAv) {
        var id = UUID.randomUUID();
        var fho = fhoData.getForhaandsorientering();
        database.update("INSERT INTO FORHAANDSORIENTERING(ID, AKTOR_ID, AKTIVITET_ID, AKTIVITET_VERSJON, ARENAAKTIVITET_ID, TYPE, TEKST, OPPRETTET_DATO, OPPRETTET_AV)" +
                        "VALUES (?,?,?,?,?,?,?, CURRENT_TIMESTAMP,?)",
                id.toString(),
                aktorId.get(),
                null,
                null,
                aktivitetId,
                fho.getType().name(),
                fho.getTekst(),
                opprettetAv
        );

        LOG.info("opprettet forhåndsorientering: {} med id: {}", fhoData, id);

        return id;
    }

    public void markerSomLest(String id, Date lestDato, long lestVersjon) {
        // language=sql
        var rows = database
                .update("UPDATE FORHAANDSORIENTERING SET LEST_DATO = ?, LEST_AKTIVITET_VERSJON = ? WHERE ID = ?", lestDato, lestVersjon, id);
        if (rows!=1){
            throw new IllegalStateException("Fant ikke forhåndsorienteringen som skulle oppdateres");
        }
    }

    public Forhaandsorientering getById(String id) {
        try {
            return database.queryForObject(selectAktivitet + "WHERE ID = ?", ForhaandsorienteringDAO::map,
                    id);
        }
        catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Forhaandsorientering getFhoForAktivitet(long aktivitetId) {
        try {
            return database.queryForObject(selectAktivitet + "WHERE AKTIVITET_ID = ?", ForhaandsorienteringDAO::map,
                    aktivitetId);
        }
        catch(EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static Forhaandsorientering map(ResultSet rs) throws SQLException {
        return Forhaandsorientering.builder()
                .id(rs.getString("ID"))
                .aktorId(AktorId.of(rs.getString("AKTOR_ID")))
                .aktivitetId(rs.getString("AKTIVITET_ID"))
                .arenaaktivitetId(rs.getString("ARENAAKTIVITET_ID"))
                .aktivitetVersjon(rs.getString("AKTIVITET_VERSJON"))
                .type(EnumUtils.valueOf(Type.class, rs.getString("TYPE")))
                .tekst(rs.getString("TEKST"))
                .opprettetDato(Database.hentDato(rs, "OPPRETTET_DATO"))
                .opprettetAv(rs.getString("OPPRETTET_AV"))
                .lestDato(Database.hentDato(rs, "LEST_DATO"))

                .build();
    }

}
