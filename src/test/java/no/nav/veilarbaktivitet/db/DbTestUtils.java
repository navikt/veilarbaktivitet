package no.nav.veilarbaktivitet.db;

import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class DbTestUtils {

    private static final List<String> ALL_TABLES = Arrays.asList(
            "BRUKERNOTIFIKASJON",
            "MOTE_SMS_HISTORIKK",
            "STILLING_FRA_NAV",
            "GJELDENDE_MOTE_SMS",
            "STILLINGSSOK",
            "EGENAKTIVITET",
            "SOKEAVTALE",
            "IJOBB",
            "MOTE",
            "BEHANDLING",
            "AKTIVITET",
            "ARENA_FORHAANDSORIENTERING",
            "FORHAANDSORIENTERING",
            "shedlock"
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
        FluentConfiguration config = Flyway
                .configure()
                .dataSource(dataSource)
                .table("schema_version")
                .cleanOnValidationError(true)
                .validateMigrationNaming(true);
        Flyway flyway = new Flyway(config);
        flyway.clean();
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

}
