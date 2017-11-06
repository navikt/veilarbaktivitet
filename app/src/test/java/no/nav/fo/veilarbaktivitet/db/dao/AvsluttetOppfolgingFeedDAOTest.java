package no.nav.fo.veilarbaktivitet.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import javax.inject.Inject;

import org.junit.Test;

import no.nav.fo.IntegrasjonsTest;

public class AvsluttetOppfolgingFeedDAOTest extends IntegrasjonsTest {

    @Inject
    private AvsluttetOppfolgingFeedDAO avsluttetOppfolgingFeedDAO;

    @Test
    public void siste_kjente_id_skal_være_null_forste_gang() {
        assertThat(avsluttetOppfolgingFeedDAO.hentSisteKjenteId()).isNull();
    }

    @Test
    public void skal_ha_siste_dato_for_historiske_aktiviteter() {
        Date id = new Date();
        avsluttetOppfolgingFeedDAO.oppdaterSisteFeedId(id);
        Date sisteKjenteId = avsluttetOppfolgingFeedDAO.hentSisteKjenteId();
        assertThat(sisteKjenteId).isEqualTo(id);
    }

}