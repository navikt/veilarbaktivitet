package no.nav.fo.veilarbaktivitet.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import static java.util.Optional.ofNullable;

@Component
public class SQLUtils {

    public static final String DIALECT_PROPERTY = "dialect";
    public static final String HSQLDB_DIALECT = "hsqldb";

    @Inject
    private JdbcTemplate jdbcTemplate;

    public long nesteFraSekvens(String sekvensNavn) {
        String sekvensQuery;
        if (HSQLDB_DIALECT.equals(System.getProperty(DIALECT_PROPERTY))) {
            sekvensQuery = "call next value for " + sekvensNavn;
        } else {
            sekvensQuery = "select " + sekvensNavn + ".nextval from dual";
        }
        return jdbcTemplate.queryForObject(sekvensQuery, Long.class);
    }

    public static Date hentDato(ResultSet rs, String kolonneNavn) throws SQLException {
        return ofNullable(rs.getTimestamp(kolonneNavn))
                .map(Timestamp::getTime)
                .map(Date::new)
                .orElse(null);
    }

}
