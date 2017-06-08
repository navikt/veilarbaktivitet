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

import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.dateFromISO8601;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
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

        aktivitetDAO.slettAktivitet(aktivitet.getId());

        assertThat(aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID), empty());
    }

    @Test
    public void oppdaterAktivitet_kanOppdatereAktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val oppdatertAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId())
                .toBuilder()
                .beskrivelse("ny beskrivelse")
                .build();

//        aktivitetDAO.oppdaterAktivitet(null, oppdatertAktivitet);
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getVersjon(), not(aktivitet.getVersjon()));
    }

    @Test(expected = VersjonsKonflikt.class)
    public void oppdaterAktivitet_feilVersjon_feiler() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();
//        aktivitetDAO.oppdaterAktivitet(null, aktivitetDAO.hentAktivitet(aktivitet.getId())); // versjon oppdateres
//        aktivitetDAO.oppdaterAktivitet(null, aktivitet);
    }

    @Test
    public void endre_aktivitet_status() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

//        aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.GJENNOMFORT, "fordi");
        assertThat(aktivitetDAO.hentAktivitet(aktivitet.getId()).getStatus(), equalTo(AktivitetStatus.GJENNOMFORT));

//        aktivitetDAO.endreAktivitetStatus(aktivitet.getId(), AktivitetStatus.AVBRUTT, "fordi");
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

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET);
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet1.build(), opprettet1);
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet2.build(), opprettet2);

//        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
//        assertThat(hentetAktiviteter.size(), is(2));
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_hente_bare_en() {
        val fra = dateFromISO8601("2010-12-03T10:15:30.1+02:00");
        val opprettet1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet2 = dateFromISO8601("2010-12-03T10:15:30.2+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING);
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET);
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet1.build(), opprettet1);
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet2.build(), opprettet2);
//
//        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
//        assertThat(hentetAktiviteter.size(), is(1));
//        assertThat(hentetAktiviteter.get(0).getOpprettetDato(), is(opprettet2));
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_returnere_tom_liste() {
        val fra = dateFromISO8601("2010-12-05T11:15:30+02:00");
        val opprettet1 = dateFromISO8601("2010-12-03T10:15:30+02:00");
        val opprettet2 = dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = nyAktivitet().aktivitetType(JOBBSOEKING).build();
        val aktivitet2 = nyAktivitet().aktivitetType(EGENAKTIVITET).build();
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet1, opprettet1);
//        aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet2, opprettet2);
//
//        val hentetAktiviteter = aktivitetDAO.hentAktiviteterEtterTidspunkt(fra);
//        assertThat(hentetAktiviteter.size(), is(0));
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitetBuilder = nyAktivitet().aktivitetType(JOBBSOEKING);
        val stillingsok = nyttStillingssøk();

        aktivitetBuilder.stillingsSoekAktivitetData(stillingsok);
        val aktivitet = aktivitetBuilder.build();
//        val id = aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet);
//        return aktivitet.toBuilder().id(id).build();
        return null;
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData()
                        .setHensikt("nada"));

        val aktivitet = aktivitetBuilder.build();
//        val id = aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet);
//        return aktivitet.toBuilder().id(id).build();
        return null;

    }

    private AktivitetData gitt_at_det_finnes_en_sokeavtale() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(SOKEAVTALE)
                .sokeAvtaleAktivitetData(new SokeAvtaleAktivitetData()
                        .setAntall(10L)
                        .setAvtaleOppfolging("Oppfølging"));

        val aktivitet = aktivitetBuilder.build();
//        val id = aktivitetDAO.opprettAktivitet(AKTOR_ID, aktivitet);
//        return aktivitet.toBuilder().id(id).build();
        return null;
    }
}