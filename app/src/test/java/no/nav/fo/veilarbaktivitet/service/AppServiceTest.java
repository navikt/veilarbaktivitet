package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.TestCase.fail;
import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AppServiceTest {

    private static final long AKTIVITET_ID = 1L;

    @InjectMocks
    private AppService appService;

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Test
    public void skal_ikke_kunne_endre_aktivitet_status_fra_avbrutt_eller_fullfort() {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData())
                .setStatus(AktivitetStatus.AVBRUTT);
        when(aktivitetDAO.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitet);

        try {
            appService.oppdaterStatus(AKTIVITET_ID, AktivitetStatus.GJENNOMFORT);
            fail();
        } catch (IllegalArgumentException e) {
            verify(aktivitetDAO, never()).endreAktivitetStatus(AKTIVITET_ID, AktivitetStatus.GJENNOMFORT);
            assertThat(e.getMessage(), containsString("Kan ikke"));
        }
    }

}