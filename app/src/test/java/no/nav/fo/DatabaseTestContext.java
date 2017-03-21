package no.nav.fo;

import no.nav.fo.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static no.nav.fo.veilarbaktivitet.db.Database.DIALECT_PROPERTY;
import static no.nav.fo.veilarbaktivitet.db.Database.HSQLDB_DIALECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    public static SingleConnectionDataSource buildDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setDriverClassName(TestDriver.class.getName());
        dataSource.setUrl(TestDriver.URL);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        createTables(dataSource);
        System.setProperty(DIALECT_PROPERTY, HSQLDB_DIALECT);
        return dataSource;
    }

    private static void createTables(SingleConnectionDataSource singleConnectionDataSource) {
        try (Connection conn = singleConnectionDataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("SET DATABASE SQL SYNTAX ORA TRUE;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Flyway flyway = new Flyway();
        flyway.setLocations("db/migration/veilarbaktivitetDataSource");
        flyway.setDataSource(singleConnectionDataSource);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));
    }

}
