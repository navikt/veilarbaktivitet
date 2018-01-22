package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.db.rowmappers.KvpDataRowMapper;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Component
public class KvpDAO {

    @Inject
    private Database database;

    /**
     * Insert a new row into the database.
     */
    public void insert(KvpDTO k) {
        String sql = "" +
                "INSERT INTO KVP " +
                "(serial, kvp_id, aktor_id, enhet, opprettet_dato, avsluttet_dato) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        database.update(
                sql,
                k.getSerial(),
                k.getKvpId(),
                k.getAktorId(),
                k.getEnhet(),
                k.getOpprettetDato(),
                k.getAvsluttetDato()
        );
    }

    /**
     * Returns the highest serial in the KVP table.
     */
    public long currentSerial() {
        try {
            return database.queryForObject("SELECT MAX(SERIAL) FROM KVP", long.class);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * Check if an actor has an active KVP period, and return the office ID.
     * In case there is no active KVP period, NULL is returned.
     */
    @Nullable
    public String enhetID(String aktorId) {
        String sql = "" +
                "SELECT * FROM " +
                "(SELECT * FROM KVP WHERE AKTOR_ID = ? ORDER BY SERIAL DESC) " +
                "WHERE ROWNUM = 1";

        List<KvpDTO> results = database.query(sql, KvpDataRowMapper::map, aktorId);

        if (results.size() == 0) {
            return null;
        }

        KvpDTO kvp = results.stream().findFirst().get();
        Date dt = kvp.getAvsluttetDato();

        // If end date is not set, or is set in the future, return the office ID.
        if (dt == null || dt.after(new Date())) {
            return kvp.getEnhet();
        }

        // The latest KVP period has finished.
        return null;
    }
}
