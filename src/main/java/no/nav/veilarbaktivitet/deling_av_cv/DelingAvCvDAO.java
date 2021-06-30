package no.nav.veilarbaktivitet.deling_av_cv;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class DelingAvCvDAO {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public boolean eksistererDelingAvCv(String bestillingsId) {
        SqlParameterSource bestillingsIdParameter = new MapSqlParameterSource("bestillingsId", bestillingsId);
        // language=sql
        return !jdbcTemplate.queryForList("SELECT BESTILLINGSID FROM STILLING_FRA_NAV WHERE BESTILLINGSID=:bestillingsId ", bestillingsIdParameter, String.class).isEmpty();
    }
}
