package no.nav.veilarbaktivitet.db.dao;

import java.util.Date;
import no.nav.veilarbaktivitet.db.Database;
import org.springframework.stereotype.Component;

@Component
public class AvsluttetOppfolgingFeedDAO {
	private final Database database;

	public AvsluttetOppfolgingFeedDAO(Database database) {
		this.database = database;
	}

	public Date hentSisteKjenteId() {
		return database.queryForObject(
			"SELECT SISTE_FEED_ELEMENT_ID FROM FEED_METADATA",
			rs -> Database.hentDato(rs, "SISTE_FEED_ELEMENT_ID")
		);
	}

	public void oppdaterSisteFeedId(Date id) {
		database.update("UPDATE FEED_METADATA SET SISTE_FEED_ELEMENT_ID = ?", id);
	}
}
