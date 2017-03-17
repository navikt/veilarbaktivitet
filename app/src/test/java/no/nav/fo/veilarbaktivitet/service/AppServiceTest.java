package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.dao.EndringsLoggDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatusData;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AppServiceTest {

    @InjectMocks
    private AppService appService;

//    @Mock
//    private AktoerConsumer aktoerConsumerMock;

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private EndringsLoggDAO endringsLoggDAO;

//    @Before
//    public void setup() {
//        when(aktoerConsumerMock.hentAktoerIdForIdent(KJENT_IDENT)).thenReturn(Optional.of(KJENT_AKTOR_ID));
//    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_status_fra_avbrutt_eller_fullfort() {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData())
                .setStatus(AktivitetStatusData.AVBRUTT);

        val aktivitetId = 1L;

        when(aktivitetDAO.hentAktivitet(aktivitetId)).thenReturn(aktivitet);
        verify(aktivitetDAO, never()).endreAktivitetStatus(aktivitetId, AktivitetStatusData.GJENNOMFORT);

        appService.oppdaterStatus(aktivitetId, AktivitetStatusData.GJENNOMFORT);
    }


}