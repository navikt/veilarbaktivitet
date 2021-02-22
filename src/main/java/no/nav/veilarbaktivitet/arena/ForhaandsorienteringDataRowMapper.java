package no.nav.veilarbaktivitet.arena;

import lombok.val;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.util.EnumUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ForhaandsorienteringDataRowMapper {
    public static ArenaForhaandsorienteringData mapForhaandsorientering(ResultSet rs) throws SQLException {
        val forhaandsorientering = ArenaForhaandsorienteringData
                .builder()
                .arenaktivitetId(rs.getString("arenaaktivitet_id"))
                .aktorId(rs.getString("aktor_id"))
                .forhaandsorientering(Forhaandsorientering
                        .builder()
                        .type(EnumUtils.valueOf(Forhaandsorientering.Type.class, rs.getString("fho_type")))
                        .tekst(rs.getString("fho_tekst"))
                        .build())
                .opprettetDato(Database.hentDato(rs, "fho_opprettet_dato"));

        return forhaandsorientering.build();
    }
}
