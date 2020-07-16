package no.nav.veilarbaktivitet.db;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class MigrerDatabaseTest extends DatabaseTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Test
    public void kanQueryeDatabasen() {
        assertThat(jdbcTemplate.queryForList("SELECT * FROM AKTIVITET"), empty());
    }

}