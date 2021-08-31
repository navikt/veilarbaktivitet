package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
class JobbDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<JobbDTO> hentJobber() {
        String sql = "SELECT * FROM JOBB WHERE STATUS='PENDING';";
        return jdbcTemplate.query(sql, JobbDAO::mapper);
    }

    @SneakyThrows
    private static JobbDTO mapper(ResultSet rs, int i) {
        return JobbDTO.builder()
                .id(rs.getLong("id"))
                .aktivitetId(rs.getLong("Aktivitet_Id"))
                .versjon(rs.getLong("versjon"))
                .jobbType(JobbType.valueOf(rs.getString("JobbType")))
                .status(Status.valueOf(rs.getString("Status")))
                .build();
    }

    public JobbDTO insertJobb(JobbDTO jobb) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobbtype", jobb.getJobbType().name())
                .addValue("aktivitet_id", jobb.getAktivitetId())
                .addValue("versjon", jobb.getVersjon())
                .addValue("status", Status.PENDING.name());
        String sql = "INSERT INTO JOBB (" +
                "jobbtype," +
                "aktivitet_id," +
                "versjon," +
                "status" +
                ") VALUES (" +
                ":jobbtype," +
                ":aktivitet_id," +
                ":versjon," +
                ":status);";
        jdbcTemplate.update(sql, params, keyHolder);
        return jobb.withId(keyHolder.getKey().intValue());
    }
}
