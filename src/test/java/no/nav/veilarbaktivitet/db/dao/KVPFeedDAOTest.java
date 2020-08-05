package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.DatabaseTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KVPFeedDAOTest extends DatabaseTest {

    private KVPFeedDAO kvpFeedDAO;

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
