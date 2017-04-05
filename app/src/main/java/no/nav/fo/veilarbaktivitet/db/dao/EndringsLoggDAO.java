package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.domain.EndringsloggData;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EndringsLoggDAO {

    private static final Logger LOG = getLogger(EndringsLoggDAO.class);

    @Inject
    private Database database;

    public List<EndringsloggData> hentEndringdsloggForAktivitetId(long aktivitetId) {
        return database.query("SELECT * FROM ENDRINGSLOGG WHERE aktivitet_id = ? ORDER BY id desc",
                this::mapEndringsLogg,
                aktivitetId
        );
    }

    private EndringsloggData mapEndringsLogg(ResultSet rs) throws SQLException {
        return new EndringsloggData()
                .setEndretDato(hentDato(rs, "endrings_dato"))
                .setEndretAv(rs.getString("endret_av"))
                .setEndringsBeskrivelse(rs.getString("endrings_beskrivelse"))
                ;
    }


    public void opprettEndringsLogg(long aktivitetId, String endretAv, String endringsBeskrivelse) {
        long endringsLoggId = database.nesteFraSekvens("ENDRINGSLOGG_ID_SEQ");
        database.update("INSERT INTO ENDRINGSLOGG(id, aktivitet_id, " +
                        "endrings_dato, endret_av, endrings_beskrivelse) " +
                        "VALUES (?,?,?,?,?)",
                endringsLoggId,
                aktivitetId,
                new Date(),
                endretAv,
                endringsBeskrivelse);

        LOG.info("opprettet endringslogg with id: {}", endringsLoggId);
    }

    public int slettEndringslogg(long aktivitetId) {
        return database.update("DELETE FROM ENDRINGSLOGG WHERE aktivitet_id = ?",
                aktivitetId
        );
    }



}
