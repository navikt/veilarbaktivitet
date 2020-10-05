package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.Database;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
public class AvsluttetOppfolgingFeedDAO {

    private final Database database;

    public AvsluttetOppfolgingFeedDAO(Database database) {
        this.database = database;
    }

    public ZonedDateTime hentSisteKjenteId() {
        return database.queryForObject("SELECT SISTE_FEED_ELEMENT_ID FROM FEED_METADATA",
                (rs) -> Database.hentDatoMedTidssone(rs, "SISTE_FEED_ELEMENT_ID")
        );

    }

    public void oppdaterSisteFeedId(ZonedDateTime id) {
        database.update(
                "UPDATE FEED_METADATA SET SISTE_FEED_ELEMENT_ID = ?", 
                id
        );
    }
}
