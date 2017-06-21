package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;
import static org.assertj.core.api.Assertions.assertThat;

public class AktivitetFeedDAOTest extends IntegrasjonsTest {

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private AktivitetFeedDAO aktivitetFeedDAO;

    @Test
    public void hent_aktiviteter_for_feed() {
        val fra = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = aktivitetFeedDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter).hasSize(2);
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_hente_bare_en() {
        val fra = dateFromISO8601("2010-12-03T10:15:30.1+02:00");
        val endret1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = dateFromISO8601("2010-12-03T10:15:30.2+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = aktivitetFeedDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter).hasSize(1);
        assertThat(hentetAktiviteter.get(0).getEndretDato()).isEqualTo(endret2);
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_returnere_tom_liste() {
        val fra = dateFromISO8601("2010-12-05T11:15:30+02:00");
        val endret1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = aktivitetFeedDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter).isEmpty();
    }

    @Test
    public void skal_returnere_null_hvis_ingen_historiske_i_databasen() {
        val aktivitet = aktivitetFeedDAO.hentSisteHistoriskeTidspunkt();
        assertThat(aktivitet).isNull();
    }

    @Test
    public void skal_ha_siste_dato_for_historiske_aktiviteter() {
        val endret1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING).historiskDato(endret1);
        val aktivitet2 = nyAktivitet().aktivitetType(JOBBSOEKING).historiskDato(endret2);

        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val sisteHistoriskeTidspunkt = aktivitetFeedDAO.hentSisteHistoriskeTidspunkt();
        assertThat(sisteHistoriskeTidspunkt).isEqualTo(endret2);
    }
}