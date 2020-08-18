package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static no.nav.veilarbaktivitet.db.DbTestUtils.createTestDataSource;
import static no.nav.veilarbaktivitet.db.DbTestUtils.initDb;

public class LocalH2Database {

    private static JdbcTemplate db;

    public static JdbcTemplate getDb() {
        if (db == null) {
            TestDriver.init();
            DataSource dataSource = createTestDataSource("jdbc:h2:mem:veilarbaktivitet-local;DB_CLOSE_DELAY=-1;MODE=Oracle;");
            db = new JdbcTemplate(dataSource);
            initDb(dataSource);
        }


        return db;
    }
}
