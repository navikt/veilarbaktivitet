package no.nav.fo.veilarbaktivitet.db.dao;

import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

public class KVPFeedDAOTest extends IntegrasjonsTest {

    @Inject
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
