package no.nav.fo.veilarbaktivitet.db.rowmappers;

import no.nav.fo.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;

import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.fo.veilarbaktivitet.db.Database.hentDato;
import static no.nav.fo.veilarbaktivitet.util.EnumUtils.valueOf;

public class AktivitetFeedDataRowMapper {
    public static AktivitetFeedData mapAktivitetForFeed(ResultSet rs) throws SQLException {
        long aktivitetId = rs.getLong("aktivitet_id");
        return new AktivitetFeedData()
                .setAktivitetId(String.valueOf(aktivitetId))
                .setAktorId(rs.getString("aktor_id"))
                .setAktivitetType(AktivitetTypeData.valueOf(rs.getString("type")))
                .setFraDato(hentDato(rs, "fra_dato"))
                .setTilDato(hentDato(rs, "til_dato"))
                .setStatus(valueOf(AktivitetStatus.class, rs.getString("status")))
                .setOpprettetDato(hentDato(rs, "opprettet_dato"))
                .setAvtalt(rs.getBoolean("avtalt"));
    }
}
