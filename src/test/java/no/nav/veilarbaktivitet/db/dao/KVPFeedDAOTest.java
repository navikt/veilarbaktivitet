package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class KVPFeedDAOTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private KVPFeedDAO kvpFeedDAO = new KVPFeedDAO(database);

    @Test
    public void siste_kjente_id_skal_være_0_forste_gang() {
        assertThat(kvpFeedDAO.hentSisteKVPFeedId()).isEqualTo(0L);
    }

    @Test
    public void skal_kunne_sette_og_hente_siste_kjente_id() {
        kvpFeedDAO.oppdaterSisteKVPFeedId(2);
        assertThat(kvpFeedDAO.hentSisteKVPFeedId()).isEqualTo(2L);
    }

}
