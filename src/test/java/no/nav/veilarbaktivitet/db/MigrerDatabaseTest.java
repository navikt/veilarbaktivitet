package no.nav.veilarbaktivitet.db;

import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.db.testdriver.TestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static no.nav.veilarbaktivitet.db.DbTestUtils.createTestDataSource;
import static no.nav.veilarbaktivitet.db.DbTestUtils.initDb;
import static org.assertj.core.api.Assertions.assertThat;

class MigrerDatabaseTest {
    private static final AtomicInteger databaseCounter = new AtomicInteger();
    private JdbcTemplate db;

    @BeforeEach
    public void setupIsolatedDatabase() {
        TestDriver.init();

        String dbUrl = format("jdbc:h2:mem:veilarboppfolging-local-%d;DB_CLOSE_DELAY=-1;MODE=Oracle;", databaseCounter.incrementAndGet());
        DataSource testDataSource = createTestDataSource(dbUrl);

        initDb(testDataSource);

        db = new JdbcTemplate(testDataSource);
    }

    @Test
    void kanQueryeDatabasen() {
        assertThat(db.queryForList("SELECT * FROM AKTIVITET")).isEmpty();
    }

    @AfterEach
    @SneakyThrows
    public void shutdownIsolatedDatabase() {
        Connection connection = Objects.requireNonNull(db.getDataSource()).getConnection();
        connection.createStatement().execute("SHUTDOWN");
        connection.close();
    }
}