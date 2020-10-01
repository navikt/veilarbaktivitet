package no.nav.veilarbaktivitet.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Optional.ofNullable;

@Component
public class Database {

    private JdbcTemplate jdbcTemplate;

    public Database(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> List<T> query(String sql, Mapper<T> mapper, Object... args) {
        return jdbcTemplate.query(sql, mapper, args);
    }

    public int update(String sql, Object... args) {
        return jdbcTemplate.update(sql, args);
    }

    public <T> T queryForObject(String sql, Mapper<T> mapper, Object... args) {
        return jdbcTemplate.queryForObject(sql, mapper, args);
    }

    public <T> T queryForObject(String sql, Class<T> cls) {
        return jdbcTemplate.queryForObject(sql, cls);
    }

    public long nesteFraSekvens(String sekvensNavn) {
        return jdbcTemplate.queryForObject("select " + sekvensNavn + ".nextval from dual", Long.class);
    }

    public static ZonedDateTime hentDatoMedTidssone(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(timestamp -> timestamp.toLocalDateTime().atZone(ZoneId.systemDefault()))
                .orElse(null);
    }

    @FunctionalInterface
    public interface Mapper<T> extends RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;

        @Override
        default T mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return map(resultSet);
        }

    }

}
