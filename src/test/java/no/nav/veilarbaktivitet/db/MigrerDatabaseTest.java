package no.nav.veilarbaktivitet.db;

import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class MigrerDatabaseTest extends DatabaseTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();

    @Test
    public void kanQueryeDatabasen() {
        assertThat(jdbcTemplate.queryForList("SELECT * FROM AKTIVITET"), empty());
    }

}