package no.nav.fo.veilarbaktivitet.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import javax.inject.Inject;

import no.nav.fo.veilarbaktivitet.db.DatabaseTest;
import org.junit.Test;

public class AvsluttetOppfolgingFeedDAOTest extends DatabaseTest {

    @Inject
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