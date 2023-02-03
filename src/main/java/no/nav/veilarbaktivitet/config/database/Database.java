package no.nav.veilarbaktivitet.config.database;

import lombok.Getter;
import no.nav.common.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Component
@Getter
public class Database {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public Database(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
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

    public static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

    public static Date hentDatoDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getDate(kolonneNavn))
                .map(java.sql.Date::getTime)
                .map(Date::new)
                .orElse(null);
    }

    public static ZonedDateTime hentZonedDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return  ofNullable(rs.getTimestamp(kolonneNavn))
                .map(java.sql.Timestamp::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null);
    }

    public static LocalDateTime hentLocalDateTime(ResultSet rs, String kolonneNavn) throws SQLException {
        return  ofNullable(rs.getTimestamp(kolonneNavn))
                .map(java.sql.Timestamp::toInstant)
                .map(instant ->  LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))
                .orElse(null);
    }

    public static UUID hentMaybeUUID(ResultSet rs, String kolonneNavn) throws SQLException {
        String uuid = rs.getString(kolonneNavn);

        if (StringUtils.isEmpty(uuid)) {
            return null;
        }

       try {
        return UUID.fromString(uuid);
       } catch (IllegalArgumentException e) {
           return  null;
       }
    }

    public static <T> T hentObjectFromJsonString(ResultSet rs, String kolonneNavn, Class<T> valueClass) throws SQLException {
        String json = rs.getString(kolonneNavn);
        if (json == null) return null;

       return JsonUtils.fromJson(json, valueClass);
    }

    public static <T> List<T>  hentListObjectFromJsonString(ResultSet rs, String kolonneNavn, Class<T> valueClass) throws SQLException {
        String json = rs.getString(kolonneNavn);
        if (json == null) return List.of();

        return JsonUtils.fromJsonArray(json, valueClass);
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
