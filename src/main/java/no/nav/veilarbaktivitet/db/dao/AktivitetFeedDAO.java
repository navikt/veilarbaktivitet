package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.rowmappers.AktivitetFeedDataRowMapper;
import no.nav.veilarbaktivitet.domain.AktivitetFeedData;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class AktivitetFeedDAO {

    private final Database database;

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

}
