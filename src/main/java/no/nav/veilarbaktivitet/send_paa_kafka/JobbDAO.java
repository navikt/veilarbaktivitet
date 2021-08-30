package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class JobbDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<AktivitetsJobb> hentJobber() {
        String sql = "SELECT * FROM JOBB WHERE STATUS='PENDING';";
        MapSqlParameterSource params = null;
        return jdbcTemplate.queryForList(sql, params, AktivitetsJobb.class);
    }


}
