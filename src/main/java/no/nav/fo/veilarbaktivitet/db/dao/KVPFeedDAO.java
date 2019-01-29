package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class KVPFeedDAO {

    private final Database database;

    @Inject
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
