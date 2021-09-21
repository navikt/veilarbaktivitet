package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.db.rowmappers.AktivitetDataRowMapper;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class DelingAvCvDAO {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public boolean eksistererDelingAvCv(String bestillingsId) {
        SqlParameterSource bestillingsIdParameter = new MapSqlParameterSource("bestillingsId", bestillingsId);
        // language=sql
        return !jdbcTemplate.queryForList("SELECT BESTILLINGSID FROM STILLING_FRA_NAV WHERE BESTILLINGSID=:bestillingsId ", bestillingsIdParameter, String.class).isEmpty();
    }

    public List<AktivitetData> hentAktiviteterSomSkalAvbrytes(long maxAntall) {
        SqlParameterSource parameter = new MapSqlParameterSource("maxAntall", maxAntall);
        return jdbcTemplate.query("" +
                        " SELECT * " +
                        " FROM AKTIVITET " +
                        " JOIN STILLING_FRA_NAV ON AKTIVITET.AKTIVITET_ID = STILLING_FRA_NAV.AKTIVITET_ID AND AKTIVITET.VERSJON = STILLING_FRA_NAV.VERSJON " +
                        " WHERE GJELDENDE = 1 " +
                        " AND LIVSLOPSTATUS_KODE != 'AVBRUTT' " +
                        " AND AKTIVITET_TYPE_KODE  = 'STILLING_FRA_NAV' " +
                        " AND SVARFRIST < current_timestamp " +
                        " order by AKTIVITET.AKTIVITET_ID" +
                        " fetch first :maxAntall rows only ",
                parameter,
                new AktivitetDataRowMapper());
    }
}

