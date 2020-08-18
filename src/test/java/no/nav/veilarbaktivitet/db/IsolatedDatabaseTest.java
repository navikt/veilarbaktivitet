package no.nav.veilarbaktivitet.db;

import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static no.nav.veilarbaktivitet.db.DbTestUtils.createTestDataSource;
import static no.nav.veilarbaktivitet.db.DbTestUtils.initDb;

/**
 * Creates and shuts down a new database for each test
 */
public abstract class IsolatedDatabaseTest {

    private static final AtomicInteger databaseCounter = new AtomicInteger();

    protected JdbcTemplate db;

    @Before
    public void setupIsolatedDatabase() {
        TestDriver.init();

        String dbUrl = format("jdbc:h2:mem:veilarboppfolging-local-%d;DB_CLOSE_DELAY=-1;MODE=Oracle;", databaseCounter.incrementAndGet());
        DataSource testDataSource = createTestDataSource(dbUrl);

        initDb(testDataSource);

        db = new JdbcTemplate(testDataSource);
    }

    @After
    @SneakyThrows
    public void shutdownIsolatedDatabase() {
        Connection connection = db.getDataSource().getConnection();
        connection.createStatement().execute("SHUTDOWN");
        connection.close();
    }

}
