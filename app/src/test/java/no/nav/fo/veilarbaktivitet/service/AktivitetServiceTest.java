package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;

import static junit.framework.TestCase.fail;
import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktivitetServiceTest {

    private static final long AKTIVITET_ID = 69L;

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Captor
    private ArgumentCaptor argumentCaptor;

    @InjectMocks
    private AktivitetService aktivitetService;

    @Test
    public void opprettAktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .build();

        when(aktivitetDAO.getNextUniqueAktivitetId()).thenReturn(AKTIVITET_ID);

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet);

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getId(), equalTo(AKTIVITET_ID));
        assertThat(capturedAktiviet.getFraDato(), equalTo(aktivitet.getFraDato()));
        assertThat(capturedAktiviet.getTittel(), equalTo(aktivitet.getTittel()));

        assertThat(capturedAktiviet.getAktorId(), equalTo(KJENT_AKTOR_ID));
        assertThat(capturedAktiviet.getTransaksjonsTypeData(), equalTo(TransaksjonsTypeData.OPPRETTET));
        assertThat(capturedAktiviet.getOpprettetDato(), notNullValue());
    }

    @Test
    public void oppdaterStatus() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .id(AKTIVITET_ID)
                .egenAktivitetData(new EgenAktivitetData());

        val aktivitet = aktivitetBuilder.build();
        when(aktivitetDAO.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitet);

        val avsluttKommentar = "Alexander er best";
        val nyStatus = AktivitetStatus.GJENNOMFORT;
        val oppdatertAktivitet = aktivitetBuilder
                .beskrivelse("ikke rett beskrivelse")
                .avsluttetKommentar(avsluttKommentar)
                .status(nyStatus)
                .build();
        aktivitetService.oppdaterStatus(oppdatertAktivitet);

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));

        assertThat(capturedAktiviet.getStatus(), equalTo(nyStatus));
        assertThat(capturedAktiviet.getAvsluttetKommentar(), equalTo(avsluttKommentar));
    }

    @Test
    public void oppdaterEtikett() {
        val stilling = nyttStillingssøk();
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .id(AKTIVITET_ID)
                .stillingsSoekAktivitetData(stilling);

        val aktivitet = aktivitetBuilder.build();
        when(aktivitetDAO.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitet);

        val oppdatertAktivitet = aktivitetBuilder
                .beskrivelse("Alexander er fremdeles best")
                .stillingsSoekAktivitetData(stilling.setStillingsoekEtikett(StillingsoekEtikettData.AVSLAG))
                .build();
        aktivitetService.oppdaterEtikett(oppdatertAktivitet);

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));

        assertThat(capturedAktiviet.getStillingsSoekAktivitetData().getStillingsoekEtikett(),
                equalTo(StillingsoekEtikettData.AVSLAG));
    }

    @Test
    public void oppdaterAktivitetFrist() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .id(AKTIVITET_ID)
                .egenAktivitetData(new EgenAktivitetData());

        val aktivitet = aktivitetBuilder.build();

        val nyFrist = new Date();
        aktivitetService.oppdaterAktivitetFrist(aktivitet, nyFrist);

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getTilDato(), equalTo(nyFrist));
    }

    @Test
    public void oppdaterAktivitet() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .id(AKTIVITET_ID);

        val nyBeskrivelse = "Alexander er den beste";
        val nyLenke = "www.alexander-er-best.no";
        val oppdatertAktivitet = aktivitetBuilder
                .beskrivelse(nyBeskrivelse)
                .lenke(nyLenke)
                .build();
        aktivitetService.oppdaterAktivitet(aktivitetBuilder.build(), oppdatertAktivitet);

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getBeskrivelse(), equalTo(nyBeskrivelse));
        assertThat(capturedAktiviet.getLenke(), equalTo(nyLenke));
    }

    @Test(expected = VersjonsKonflikt.class)
    public void oppdaterAktivitet_skal_gi_versjonsKonflikt_hvis_to_oppdaterer_aktiviteten_samtidig() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .id(AKTIVITET_ID);

        val aktivitet = aktivitetBuilder.build();

        doThrow(new DuplicateKeyException("versjon fins")).when(aktivitetDAO).insertAktivitet(any());

        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet);
    }

    @Test
    public void oppdaterAktivitet_skal_sette_rett_transaksjonstype() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .id(AKTIVITET_ID);

        val nyBeskrivelse = "Alexander er den beste";
        val nyLenke = "www.alexander-er-best.no";
        val oppdatertAktivitet = aktivitetBuilder
                .beskrivelse(nyBeskrivelse)
                .lenke(nyLenke);
        aktivitetService.oppdaterAktivitet(aktivitetBuilder.build(), oppdatertAktivitet.build());

        Mockito.verify(aktivitetDAO, times(1)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet = ((AktivitetData) argumentCaptor.getValue());

        assertThat(capturedAktiviet.getTransaksjonsTypeData(), equalTo(TransaksjonsTypeData.DETALJER_ENDRET));

        aktivitetService.oppdaterAktivitet(aktivitetBuilder.build(), oppdatertAktivitet.avtalt(true).build());
        Mockito.verify(aktivitetDAO, times(2)).insertAktivitet((AktivitetData) argumentCaptor.capture());
        val capturedAktiviet2 = ((AktivitetData) argumentCaptor.getValue());
        assertThat(capturedAktiviet2.getTransaksjonsTypeData(), equalTo(TransaksjonsTypeData.AVTALT));
    }


    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitetBuilder = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .status(AktivitetStatus.AVBRUTT)
                .id(AKTIVITET_ID);
        when(aktivitetDAO.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitetBuilder.build());

        try {
            aktivitetService.oppdaterStatus(aktivitetBuilder.build());
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterEtikett(aktivitetBuilder.build());
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterAktivitetFrist(aktivitetBuilder.build(), new Date());
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterAktivitet(aktivitetBuilder.build(), aktivitetBuilder.build());
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        verify(aktivitetDAO, never()).insertAktivitet(any());

    }

}