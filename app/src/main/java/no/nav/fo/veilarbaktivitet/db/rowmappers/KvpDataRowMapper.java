package no.nav.fo.veilarbaktivitet.db.rowmappers;

import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

public class KvpDataRowMapper {
    public static KvpDTO map(ResultSet rs) throws SQLException {
        return new KvpDTO()
                .setKvpId(rs.getLong("kvp_id"))
                .setSerial(rs.getLong("serial"))
                .setAktorId(rs.getString("aktor_id"))
                .setEnhet(rs.getString("enhet"))
                .setOpprettetDato(rs.getTimestamp("opprettet_dato"))
                .setAvsluttetDato(rs.getTimestamp("avsluttet_dato"));
    }
}
