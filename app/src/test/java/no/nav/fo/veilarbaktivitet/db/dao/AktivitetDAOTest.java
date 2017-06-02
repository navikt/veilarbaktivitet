package no.nav.fo.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import no.nav.fo.veilarbaktivitet.domain.SokeAvtaleAktivitetData;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class AktivitetDAOTest extends IntegrasjonsTest {

    private static final String AKTOR_ID = "1234";

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Test
    public void opprette_og_hente_egenaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_sokeavtaleaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_sokeavtale();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void slett_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val antallSlettet = aktivitetDAO.slettAktivitet(aktivitet.getId());

        assertThat(antallSlettet, equalTo(1));
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID), empty());
    }

    @Test
    public void oppdaterAktivitet_kanOppdatereAktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();
        aktivitetDAO.oppdaterAktivitet(aktivitetDAO.hentAktivitet(aktivitet.getId()).setBeskrivelse("ny beskrivelse"));
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getVersjon(), not(aktivitet.getVersjon()));
    }

    @Test(expected = VersjonsKonflikt.class)
    public void oppdaterAktivitet_feilVersjon_feiler() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();
        aktivitetDAO.oppdaterAktivitet(aktivitetDAO.hentAktivitet(aktivitet.getId())); // versjon oppdateres
        aktivitetDAO.oppdaterAktivitet(aktivitet);
    }

    @Test
    public void endre_aktivitet_status() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val raderEndret = aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.GJENNOMFORT, "fordi");
        assertThat(raderEndret, equalTo(1));
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getStatus(), equalTo(AktivitetStatus.GJENNOMFORT));

        val raderEndret2 = aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.AVBRUTT, "fordi");
        assertThat(raderEndret2, equalTo(1));
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getStatus(), equalTo(AktivitetStatus.AVBRUTT));
    }

    @Test
    public void hent_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(aktivitet, equalTo(hentetAktivitet));
    }

    @Test
    public void hent_aktiviteter_for_feed() {
        val fra = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet(AKTOR_ID).setAktivitetType(EGENAKTIVITET);
        aktivitetDAO.opprettAktivitet(aktivitet1, opprettet1);
        aktivitetDAO.opprettAktivitet(aktivitet2, opprettet2);

        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter.size(), is(2));
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_hente_bare_en() {
        val fra = dateFromISO8601("2010-12-03T10:15:30.1+02:00");
        val opprettet1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet2 = dateFromISO8601("2010-12-03T10:15:30.2+02:00");

        val aktivitet1 = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet(AKTOR_ID).setAktivitetType(EGENAKTIVITET);
        aktivitetDAO.opprettAktivitet(aktivitet1, opprettet1);
        aktivitetDAO.opprettAktivitet(aktivitet2, opprettet2);

        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter.size(), is(1));
        assertThat(hentetAktiviteter.get(0).getOpprettetDato(), is(opprettet2));
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_returnere_tom_liste() {
        val fra = dateFromISO8601("2010-12-05T11:15:30+02:00");
        val opprettet1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet(AKTOR_ID).setAktivitetType(EGENAKTIVITET);
        aktivitetDAO.opprettAktivitet(aktivitet1, opprettet1);
        aktivitetDAO.opprettAktivitet(aktivitet2, opprettet2);

        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
        assertThat(hentetAktiviteter.size(), is(0));
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = nyAktivitet(AKTOR_ID).setAktivitetType(JOBBSOEKING);
        val stillingsok = nyttStillingssøk();

        aktivitet.setStillingsSoekAktivitetData(stillingsok);
        aktivitetDAO.opprettAktivitet(aktivitet);

        return aktivitet.setVersjon(0);
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = nyAktivitet(AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData()
                        .setHensikt("nada"));

        aktivitetDAO.opprettAktivitet(aktivitet);

        return aktivitet.setVersjon(0);
    }

    private AktivitetData gitt_at_det_finnes_en_sokeavtale() {
        val aktivitet = nyAktivitet(AKTOR_ID)
                .setAktivitetType(SOKEAVTALE)
                .setSokeAvtaleAktivitetData(new SokeAvtaleAktivitetData()
                        .setAntall(10L)
                        .setAvtaleOppfolging("Oppfølging"));
        aktivitetDAO.opprettAktivitet(aktivitet);
        return aktivitet.setVersjon(0);
    }
}