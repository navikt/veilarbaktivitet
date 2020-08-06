package no.nav.veilarbaktivitet.db;

import org.junit.Test;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class MigrerDatabaseTest extends IsolatedDatabaseTest {

    @Test
    public void kanQueryeDatabasen() {
        assertThat(db.queryForList("SELECT * FROM AKTIVITET"), empty());
    }

}