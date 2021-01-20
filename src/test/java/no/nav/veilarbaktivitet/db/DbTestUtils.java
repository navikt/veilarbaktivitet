package no.nav.veilarbaktivitet.db;

import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class DbTestUtils {

    private final static List<String> ALL_TABLES = Arrays.asList(
            "AKTIVITET_SENDT_PAA_KAFKA_V3",
            "MOTE_SMS_HISTORIKK",
            "GJELDENDE_MOTE_SMS",
            "STILLINGSSOK",
            "EGENAKTIVITET",
            "SOKEAVTALE",
            "IJOBB",
            "MOTE",
            "BEHANDLING",
            "AKTIVITET"
    );

    public static void cleanupTestDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

    public static DataSource createTestDataSource(String dbUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(TestDriver.class.getName());
        dataSource.setUrl(dbUrl);
        return dataSource;
    }

    public static void initDb(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setCleanOnValidationError(true);
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

}
