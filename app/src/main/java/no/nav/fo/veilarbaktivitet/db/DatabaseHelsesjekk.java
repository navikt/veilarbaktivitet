package no.nav.fo.veilarbaktivitet.db;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.SQLException;

@Component
public class DatabaseHelsesjekk implements Helsesjekk {

    @Inject
    private JdbcTemplate jdbcTemplate;

    Logger logger = LoggerFactory.getLogger(DatabaseHelsesjekk.class);

    @Override
    public void helsesjekk() {
        jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Long.class);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        String dbUri = "Ukjent uri";
        try {
            dbUri = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
        } catch (SQLException e) {
            logger.error("Kunne ikke hente connection for datasource.", e);
        }
        return new HelsesjekkMetadata(
                "Database: " + dbUri,
                "Lokal database for VeilArbAktivitet",
                true
        );
    }

}
