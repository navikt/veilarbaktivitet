package no.nav.veilarbaktivitet.db.rowmappers;

import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.mappers.Helpers;
import no.nav.veilarbaktivitet.util.EnumUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AktivitetFeedDataRowMapper {
    public static AktivitetFeedData mapAktivitetForFeed(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        return new AktivitetFeedData()
                .setAktivitetId(String.valueOf(aktivitetId))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(Helpers.Type.getDTO(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"))))
                .setFraDato(Database.hentDato(rs, "fra_dato"))
                .setTilDato(Database.hentDato(rs, "til_dato"))
                .setHistorisk(Optional.ofNullable(Database.hentDato(rs, "historisk_dato")).isPresent())
                .setStatus(EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode")))
                .setEndretDato(Database.hentDato(rs, "endret_dato"))
                .setAvtalt(rs.getBoolean("avtalt"));
    }
}
