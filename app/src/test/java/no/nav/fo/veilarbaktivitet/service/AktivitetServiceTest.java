package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.feil.VersjonsKonflikt;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData;
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
        val aktivitet = lagEnNyAktivitet();

        when(aktivitetDAO.getNextUniqueAktivitetId()).thenReturn(AKTIVITET_ID);
        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);

        captureInsertAktivitetArgument();

        assertThat(getCapturedAktivitet().getId(), equalTo(AKTIVITET_ID));
        assertThat(getCapturedAktivitet().getFraDato(), equalTo(aktivitet.getFraDato()));
        assertThat(getCapturedAktivitet().getTittel(), equalTo(aktivitet.getTittel()));

        assertThat(getCapturedAktivitet().getAktorId(), equalTo(KJENT_AKTOR_ID));
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.OPPRETTET));
        assertThat(getCapturedAktivitet().getOpprettetDato(), notNullValue());
    }

    @Test
    public void oppdaterStatus() {
        val aktivitet = lagEnNyAktivitet();
        mockHentAktivitet(aktivitet);

        val avsluttKommentar = "Alexander er best";
        val nyStatus = AktivitetStatus.GJENNOMFORT;
        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("ikke rett beskrivelse")
                .avsluttetKommentar(avsluttKommentar)
                .status(nyStatus)
                .build();
        aktivitetService.oppdaterStatus(oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getStatus(), equalTo(nyStatus));
        assertThat(getCapturedAktivitet().getAvsluttetKommentar(), equalTo(avsluttKommentar));
    }

    @Test
    public void oppdaterEtikett() {
        val aktivitet = lagEnNyAktivitet();
        mockHentAktivitet(aktivitet);

        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er fremdeles best")
                .stillingsSoekAktivitetData(aktivitet
                        .getStillingsSoekAktivitetData()
                        .setStillingsoekEtikett(StillingsoekEtikettData.AVSLAG))
                .build();
        aktivitetService.oppdaterEtikett(oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getStillingsSoekAktivitetData().getStillingsoekEtikett(),
                equalTo(StillingsoekEtikettData.AVSLAG));
    }

    @Test
    public void oppdaterAktivitetFrist() {
        val aktivitet = lagEnNyAktivitet();

        val nyFrist = new Date();
        aktivitetService.oppdaterAktivitetFrist(aktivitet, aktivitet.toBuilder().tilDato(nyFrist).build(), null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTilDato(), equalTo(nyFrist));
    }

    @Test
    public void oppdaterAktivitet() {
        val aktivitet = lagEnNyAktivitet();
        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er den beste")
                .lenke("www.alexander-er-best.no")
                .build();
        aktivitetService.oppdaterAktivitet(aktivitet, oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(oppdatertAktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getLenke(), equalTo(oppdatertAktivitet.getLenke()));
    }

    @Test(expected = VersjonsKonflikt.class)
    public void oppdaterAktivitet_skal_gi_versjonsKonflikt_hvis_to_oppdaterer_aktiviteten_samtidig() {
        val aktivitet = lagEnNyAktivitet();
        doThrow(new DuplicateKeyException("versjon fins")).when(aktivitetDAO).insertAktivitet(any());
        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet, null);
    }

    @Test
    public void oppdaterAktivitet_skal_sette_rett_transaksjonstype() {
        val aktivitet = lagEnNyAktivitet();

        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.DETALJER_ENDRET));

        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet.toBuilder().avtalt(true).build(), null);
        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.AVTALT));
    }


    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitet = lagEnNyAktivitet().toBuilder().status(AktivitetStatus.AVBRUTT).build();
        mockHentAktivitet(aktivitet);

        try {
            aktivitetService.oppdaterStatus(aktivitet, null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterEtikett(aktivitet, null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterAktivitetFrist(aktivitet, aktivitet, null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            aktivitetService.oppdaterAktivitet(aktivitet, aktivitet, null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        verify(aktivitetDAO, never()).insertAktivitet(any());

    }

    public AktivitetData lagEnNyAktivitet() {
        val stilling = nyttStillingssøk();
        return nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .id(AKTIVITET_ID)
                .stillingsSoekAktivitetData(stilling)
                .build();
    }

    public void mockHentAktivitet(AktivitetData aktivitetData) {
        when(aktivitetDAO.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitetData);
    }

    public void captureInsertAktivitetArgument() {
        Mockito.verify(aktivitetDAO, atLeastOnce()).insertAktivitet((AktivitetData) argumentCaptor.capture());
    }

    public AktivitetData getCapturedAktivitet() {
        return ((AktivitetData) argumentCaptor.getValue());
    }

}