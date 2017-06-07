package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.veilarbaktivitet.db.Database;
import no.nav.fo.veilarbaktivitet.db.rowmappers.AktivitetFeedDataRowMapper;
import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Component
public class AktivitetFeedDAO {

    private final Database database;

    @Inject
    public AktivitetFeedDAO(Database database) {
        this.database = database;
    }

    public List<AktivitetFeedData> hentAktiviteterEtterTidspunkt(Date date) {
        return database.query(
                "SELECT " +
                        "aktivitet_id, aktor_id, type, status, fra_dato, til_dato, opprettet_dato, avtalt " +
                        "FROM aktivitet " +
                        "WHERE opprettet_dato >= ? and gjeldende = true",
                AktivitetFeedDataRowMapper::mapAktivitetForFeed,
                date
        );
    }
}
