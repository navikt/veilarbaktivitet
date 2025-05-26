package no.nav.veilarbaktivitet.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DbTestUtils {

    private static final List<String> ALL_TABLES = Arrays.asList(
            "aktivitet_brukernotifikasjon",
            "ARENA_AKTIVITET_BRUKERNOTIFIKASJON",
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
            "EKSTERNAKTIVITET",
            "AKTIVITET",
            "FORHAANDSORIENTERING",
            "SHEDLOCK",
            "ID_MAPPINGER",
            "OVERSIKTEN_MELDING_AKTIVITET_MAPPING",
            "OVERSIKTEN_MELDING_MED_METADATA"
    );

    public static void cleanupTestDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

//    public static DataSource createTestDataSource(String dbUrl) {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName(TestDriver.class.getName());
//        dataSource.setUrl(dbUrl);
//        return dataSource;
//    }
//
    public static void initDb(DataSource dataSource) {
        try(Connection connection = dataSource.getConnection()) {
            connection.prepareStatement("""
              CREATE USER veilarbaktivitet NOLOGIN;
              GRANT CONNECT on DATABASE postgres to veilarbaktivitet;
              GRANT USAGE ON SCHEMA public to veilarbaktivitet;
            """.trim()).executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Properties properties = new Properties();
        properties.put("flyway.cleanDisabled", false);
        FluentConfiguration config = Flyway
                .configure()
                .dataSource(dataSource)
                .table("schema_version")
                .configuration(properties)
                .validateMigrationNaming(true);
        Flyway flyway = new Flyway(config);
        flyway.clean();
        flyway.migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }
}
