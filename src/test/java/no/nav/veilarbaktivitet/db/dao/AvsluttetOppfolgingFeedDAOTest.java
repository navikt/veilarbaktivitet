package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.db.DatabaseTest;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class AvsluttetOppfolgingFeedDAOTest extends DatabaseTest {

    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO;

    @Test
    public void siste_kjente_id_skal_være_null_forste_gang() {
        assertThat(avsluttetOppfolgingFeedDAO.hentSisteKjenteId()).isNull();
    }

    @Test
    public void skal_kunne_sette_og_hente_siste_kjente_id() {
        Date id = new Date();
        avsluttetOppfolgingFeedDAO.oppdaterSisteFeedId(id);
        Date sisteKjenteId = avsluttetOppfolgingFeedDAO.hentSisteKjenteId();
        assertThat(sisteKjenteId).isEqualTo(id);
    }

}