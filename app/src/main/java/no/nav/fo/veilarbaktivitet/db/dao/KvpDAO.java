package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class KvpDAO {

    private static final Logger LOG = getLogger(KvpDAO.class);

    private final Database database;

    @Inject
    public KvpDAO(Database database) {
        this.database = database;
    }

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
        LOG.info("KVP cache table updated with serial {}", k.getSerial());
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
}
