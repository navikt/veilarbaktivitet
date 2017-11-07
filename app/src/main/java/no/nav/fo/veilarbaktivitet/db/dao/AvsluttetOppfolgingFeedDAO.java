package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;

@Component
public class AvsluttetOppfolgingFeedDAO {

    private final Database database;

    @Inject
    public AvsluttetOppfolgingFeedDAO(Database database) {
        this.database = database;
    }

    public Date hentSisteKjenteId() {
        return database.queryForObject("SELECT SISTE_FEED_ELEMENT_ID FROM FEED_METADATA",
                (rs) -> Database.hentDato(rs, "SISTE_FEED_ELEMENT_ID")
        );

    }

    public void oppdaterSisteFeedId(Date id) {
        database.update(
                "UPDATE FEED_METADATA SET SISTE_FEED_ELEMENT_ID = ?", 
                id
        );
    }
}
