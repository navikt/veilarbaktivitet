package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

    public JobbDTO insertJobb(JobbDTO jobb) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobbtype", jobb.getJobbType())
                .addValue("aktivitet_id", jobb.getAktivitetId())
                .addValue("versjon", jobb.getVersjon())
                .addValue("status", "PENDING");
        String sql = "INSERT INTO JOBB (" +
                "jobbtype," +
                "aktivitet_id," +
                "versjon," +
                "status" +
                ") VALUES (" +
                ":jobbtype," +
                ":aktivitet_id," +
                ":versjon," +
                "status);";
        jdbcTemplate.update(sql, params, keyHolder);
        return jobb.withId(keyHolder.getKey().intValue());
    }


}
