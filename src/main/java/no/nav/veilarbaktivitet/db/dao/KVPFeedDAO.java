package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.Database;
import org.springframework.stereotype.Component;

@Component
public class KVPFeedDAO {

    private final Database database;

    public KVPFeedDAO(Database database) {
        this.database = database;
    }

    public long hentSisteKVPFeedId() {
        return database.queryForObject("SELECT KVP_FEED_ELEMENT_ID FROM FEED_METADATA",
                (rs) -> rs.getLong("KVP_FEED_ELEMENT_ID")
        );
    }

    public void oppdaterSisteKVPFeedId(long id) {
        database.update(
                "UPDATE FEED_METADATA SET KVP_FEED_ELEMENT_ID = ?",
                id
        );
    }
}
