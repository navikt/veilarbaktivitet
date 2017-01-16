package no.nav.fo;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static no.nav.fo.veilarbaktivitet.db.SQLUtils.DIALECT_PROPERTY;
import static no.nav.fo.veilarbaktivitet.db.SQLUtils.HSQLDB_DIALECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Configuration
@EnableTransactionManagement
public class DatabaseTestContext {

    public static SingleConnectionDataSource buildDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setSuppressClose(true);
        dataSource.setDriverClassName(org.hsqldb.jdbcDriver.class.getName());
        dataSource.setUrl("jdbc:hsqldb:mem:henvendelse");
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
        flyway.setLocations("db/aktivitetDataSource");
        flyway.setDataSource(singleConnectionDataSource);
        int migrate = flyway.migrate();
        assertThat(migrate, greaterThan(0));
    }

}
