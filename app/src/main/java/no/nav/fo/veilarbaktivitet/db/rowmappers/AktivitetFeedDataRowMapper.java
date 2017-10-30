package no.nav.fo.veilarbaktivitet.db.rowmappers;

import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static no.nav.fo.veilarbaktivitet.mappers.Helpers.typeMap;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;

public class AktivitetFeedDataRowMapper {
    public static AktivitetFeedData mapAktivitetForFeed(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        return new AktivitetFeedData()
                .setAktivitetId(String.valueOf(aktivitetId))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(typeMap.get(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"))))
                .setFraDato(hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setHistorisk(Optional.ofNullable(hentDato(rs, "historisk_dato")).isPresent())
                .setStatus(valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode")))
                .setEndretDato(hentDato(rs, "endret_dato"))
                .setAvtalt(rs.getBoolean("avtalt"));
    }
}
