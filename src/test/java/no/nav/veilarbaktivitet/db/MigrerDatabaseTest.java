package no.nav.veilarbaktivitet.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrerDatabaseTest extends IsolatedDatabaseTest {

    @Test
    public void kanQueryeDatabasen() {
        assertThat(db.queryForList("SELECT * FROM AKTIVITET")).isEmpty();
    }

}