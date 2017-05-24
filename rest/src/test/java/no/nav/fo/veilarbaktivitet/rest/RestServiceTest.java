package no.nav.fo.veilarbaktivitet.rest;

import lombok.val;
import no.nav.fo.IntegrasjonsTestUtenArenaMock;
import no.nav.fo.veilarbaktivitet.AktivitetDataBuilder;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
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
import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyttStillingssøk;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class RestServiceTest extends IntegrasjonsTestUtenArenaMock {



    @Test
    public void hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter();
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void hent_aktivitet() {
        gitt_at_jeg_har_aktiviter();
        da_skal_jeg_kunne_hente_en_aktivitet();
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
    public void oppdater_etikett() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_etiketten_pa_en_aktivitet();
        da_skal_min_aktivitet_fatt_ny_etikett();
    }

    @Test
    public void hent_endrings_logg() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        nar_jeg_henter_endrings_logg_på_denne_aktiviten();
        da_skal_jeg_fa_en_endringslogg_pa_denne_aktiviteten();
    }

    @Test
    public void oppdater_aktivtet() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_en_av_aktiviten();
        da_skal_jeg_aktiviten_vare_endret();
    }


    @Test
    public void skal_ikke_kunne_endre_annet_enn_frist_pa_avtalte_aktiviter() {
        gitt_at_jeg_har_laget_en_aktivtet();
        gitt_at_jeg_har_satt_aktiviteten_til_avtalt();
        nar_jeg_lagrer_aktivteten();
        nar_jeg_oppdaterer_aktiviten();
        da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert();
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

    private AktivitetData nyStillingAktivitet() {
        return AktivitetDataBuilder.nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(AktivitetTypeData.JOBBSOEKING)
                .setStillingsSoekAktivitetData(nyttStillingssøk());
    }

    private List<AktivitetData> aktiviter = Arrays.asList(
            nyStillingAktivitet(), nyStillingAktivitet()
    );

    private AktivitetDTO aktivitet;

    private void gitt_at_jeg_har_aktiviter() {
        aktiviter.forEach(aktivitetDAO::opprettAktivitet);
    }

    private void gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet();
    }

    private void gitt_at_jeg_har_satt_aktiviteten_til_avtalt() {
        aktivitet.setAvtalt(true);
    }

    private void nar_jeg_lagrer_aktivteten() {
        aktivitet = aktivitetController.opprettNyAktivitet(aktivitet);
    }

    private AktivitetDTO orignalAktivitet;

    private void nar_jeg_oppdaterer_aktiviten() {
        orignalAktivitet = nyAktivitet()
                .setAvtalt(true)
                .setOpprettetDato(aktivitet.getOpprettetDato())
                .setFraDato(aktivitet.getFraDato())
                .setId(aktivitet.getId());

        aktivitet = aktivitetController.oppdaterAktiviet(
                aktivitet.setBeskrivelse("noe tull")
                        .setArbeidsgiver("Justice league")
                        .setEtikett(EtikettTypeDTO.AVSLAG)
                        .setTilDato(new Date())
        );
    }

    private void nar_jeg_sletter_en_aktivitet_fra_min_aktivitetsplan() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        aktivitetController.slettAktivitet(aktivitet.getId());
    }

    private AktivitetStatus nyAktivitetStatus = AktivitetStatus.AVBRUTT;
    private EtikettTypeDTO nyAktivitetEtikett = EtikettTypeDTO.AVSLAG;

    private void nar_jeg_flytter_en_aktivitet_til_en_annen_status() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        this.aktivitet = aktivitetController.oppdaterStatus(aktivitet.setStatus(nyAktivitetStatus));
    }

    private void nar_jeg_oppdaterer_etiketten_pa_en_aktivitet() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        this.aktivitet = aktivitetController.oppdaterEtikett(aktivitet.setEtikett(nyAktivitetEtikett));
    }

    private List<EndringsloggDTO> endringer;

    private void nar_jeg_henter_endrings_logg_på_denne_aktiviten() {
        endringer = aktivitetController.hentEndringsLoggForAktivitetId(aktivitet.getId());
    }

    private String nyLenke;
    private String nyAvsluttetKommentar;
    private Date oldOpprettetDato;

    private void nar_jeg_oppdaterer_en_av_aktiviten() {
        val aktivitet = this.aktiviter.get(0);
        oldOpprettetDato = aktivitet.getOpprettetDato();
        nyLenke = "itsOver9000.com";
        nyAvsluttetKommentar = "The more I talk, the more i understand why i'm single";
        aktivitet.setLenke(nyLenke);
        aktivitet.setAvsluttetKommentar(nyAvsluttetKommentar);
        aktivitet.getStillingsSoekAktivitetData().setStillingsoekEtikett(StillingsoekEtikettData.INNKALT_TIL_INTERVJU);

        this.aktivitet = aktivitetController.oppdaterAktiviet(RestMapper.mapTilAktivitetDTO(aktivitet));
    }


    private void da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetController.hentAktivitetsplan().aktiviteter;
        assertThat(aktiviteter, hasSize(2));
    }

    private void da_skal_jeg_kunne_hente_en_aktivitet() {
        assertThat(aktiviter.get(0).getId().toString(),
                equalTo(aktivitetController.hentAktivitet(aktiviter.get(0).getId().toString()).getId()));
    }

    private void da_skal_jeg_denne_aktivteten_ligge_i_min_aktivitetsplan() {
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID), hasSize(1));
    }

    private void da_skal_jeg_ha_mindre_aktiviter_i_min_aktivitetsplan() {
        assertThat(aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID), hasSize(1));
    }

    private void da_skal_min_aktivitet_fatt_ny_status() {
        assertThat(aktivitet.getStatus(), equalTo(nyAktivitetStatus));
        assertThat(aktivitetDAO.hentAktivitet(Long.parseLong(aktivitet.getId())).getStatus(), equalTo(nyAktivitetStatus));
    }

    private void da_skal_min_aktivitet_fatt_ny_etikett() {
        assertThat(aktivitet.getEtikett(), equalTo(nyAktivitetEtikett));
    }

    private void da_skal_jeg_fa_en_endringslogg_pa_denne_aktiviteten() {
        assertThat(endringer, hasSize(1));
    }

    private void da_skal_jeg_aktiviten_vare_endret() {
        assertThat(this.aktivitet.getLenke(), equalTo(nyLenke));
        assertThat(this.aktivitet.getAvsluttetKommentar(), equalTo(nyAvsluttetKommentar));
        assertThat(this.aktivitet.getEtikett(), equalTo(EtikettTypeDTO.INNKALT_TIL_INTERVJU));
        assertThat(this.aktivitet.getOpprettetDato(), equalTo(oldOpprettetDato));
    }

    private void da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert() {
        assertThat(aktivitet, equalTo(orignalAktivitet
                .setTilDato(aktivitet.tilDato)
                .setVersjon(aktivitet.versjon)
        ));
    }

    private AktivitetDTO nyAktivitet() {
        return new AktivitetDTO()
                .setTittel("tittel")
                .setBeskrivelse("beskr")
                .setLenke("lenke")
                .setType(AktivitetTypeDTO.STILLING)
                .setStatus(AktivitetStatus.GJENNOMFORT)
                .setFraDato(new Date())
                .setTilDato(new Date())
                .setKontaktperson("kontakt")
                ;
    }

}