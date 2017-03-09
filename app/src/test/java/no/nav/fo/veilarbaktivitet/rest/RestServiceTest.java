package no.nav.fo.veilarbaktivitet.rest;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.AktivitetDataBuilder;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class RestServiceTest extends IntegrasjonsTest {

    @Test
    public void hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter();
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void opprett_aktivitet() {
        gitt_at_jeg_har_laget_en_aktivtet();
        nar_jeg_lagrer_aktivteten();
        da_skal_jeg_denne_aktivteten_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void slett_aktivitet() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_sletter_en_aktivitet_fra_min_aktivitetsplan();
        da_skal_jeg_ha_mindre_aktiviter_i_min_aktivitetsplan();
    }

    @Test
    public void oppdater_status() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        da_skal_min_aktivitet_fatt_ny_status();
    }

    @Test
    public void hent_endrings_logg() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        nar_jeg_henter_endrings_logg_på_denne_aktiviten();
        da_skal_jeg_fa_en_endringslogg_pa_denne_aktiviteten();
    }

    @Inject
    private RestService aktivitetController;

    @Inject
    private MockHttpServletRequest mockHttpServletRequest;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Before
    public void setup() {
        mockHttpServletRequest.setParameter("fnr", KJENT_IDENT);
    }

    private List<AktivitetData> aktiviter = Arrays.asList(
            AktivitetDataBuilder.nyAktivitet(KJENT_AKTOR_ID),
            AktivitetDataBuilder.nyAktivitet(KJENT_AKTOR_ID)
    );

    private AktivitetDTO aktivitet;

    private void gitt_at_jeg_har_aktiviter() {
        aktiviter.forEach(aktivitetDAO::opprettAktivitet);
    }

    private void gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet();
    }

    private void nar_jeg_lagrer_aktivteten() {
        aktivitetController.opprettNyAktivitet(aktivitet);
    }

    private void nar_jeg_sletter_en_aktivitet_fra_min_aktivitetsplan() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        aktivitetController.slettAktivitet(aktivitet.getId());
    }

    private String nyAktivitetStatus = AktivitetStatusData.AVBRUTT.name();

    private void nar_jeg_flytter_en_aktivitet_til_en_annen_status() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        this.aktivitet = aktivitetController.oppdaterStatus(aktivitet.getId(), nyAktivitetStatus);
    }

    private List<EndringsloggDTO> endringer;

    private void nar_jeg_henter_endrings_logg_på_denne_aktiviten() {
        endringer = aktivitetController.hentEndringsLoggForAktivitetId(aktivitet.getId());
    }


    private void da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetController.hentAktivitetsplan().aktiviteter;
        assertThat(aktiviteter, hasSize(2));
    }

    private void da_skal_jeg_denne_aktivteten_ligge_i_min_aktivitetsplan() {
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID), hasSize(1));
    }

    private void da_skal_jeg_ha_mindre_aktiviter_i_min_aktivitetsplan() {
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID), hasSize(1));
    }

    private void da_skal_min_aktivitet_fatt_ny_status() {
        assertThat(aktivitet.getStatus(), equalTo(nyAktivitetStatus));
        assertThat(aktivitetDAO.hentAktivitet(Long.parseLong(aktivitet.getId())).getStatus().name(),
                equalTo(nyAktivitetStatus));
    }

    private void da_skal_jeg_fa_en_endringslogg_pa_denne_aktiviteten() {
        assertThat(endringer, hasSize(1));
    }


    private AktivitetDTO nyAktivitet() {
        return new AktivitetDTO()
                .setTittel("tittel")
                .setBeskrivelse("beskr")
                .setLenke("lenke")
                .setType(AktivitetTypeDTO.STILLING)
                .setStatus(AktivitetStatusData.GJENNOMFORT.name())
                .setFraDato(new Date())
                .setTilDato(new Date())
                .setKontaktperson("kontakt")
                ;
    }

}