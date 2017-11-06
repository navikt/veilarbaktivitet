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


    public List<AktivitetFeedData> hentAktiviteterEtterTidspunkt(Date date, int pageSize) {
        return database.query(
                "SELECT * FROM (" +
                        "SELECT " +
                        "aktivitet_id, aktor_id, aktivitet_type_kode, livslopstatus_kode, fra_dato, til_dato, endret_dato, avtalt, historisk_dato " +
                        "FROM aktivitet " +
                        "WHERE endret_dato >= ? and gjeldende = 1" +
                        "ORDER BY endret_dato " +
                     ") WHERE ROWNUM <= ?",
                AktivitetFeedDataRowMapper::mapAktivitetForFeed,
                date,
                pageSize
        );
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
