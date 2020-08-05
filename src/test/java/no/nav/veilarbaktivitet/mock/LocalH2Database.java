package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

public class LocalH2Database {

    private static JdbcTemplate db;

    public static JdbcTemplate getDb() {
        if (db == null) {
            TestDriver.init();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(TestDriver.class.getName());
            dataSource.setUrl("jdbc:h2:mem:veilarbaktivitet-local;DB_CLOSE_DELAY=-1;MODE=Oracle;");

            db = new JdbcTemplate(dataSource);
            initDb(dataSource);
        }


        return db;
    }

    private static void initDb(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

}
