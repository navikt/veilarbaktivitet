package no.nav.fo.veilarbaktivitet.service;

import lombok.val;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetAppServiceTest {
    private static final long AKTIVITET_ID = 666L;

    @Mock
    private ArenaAktivitetConsumer arenaAktivitetConsumer;

    @Mock
    private PepClient pepClient;

    @Mock
    private AktivitetService aktivitetService;

    @Mock
    private BrukerService brukerService;

    @InjectMocks
    private AktivitetAppService appService;

    @Before
    public void init() {
        when(brukerService.getFNRForAktorId(any())).thenReturn(Optional.of(Person.fnr("123")));
    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitet = lagEnNyAktivitet().toBuilder().status(AktivitetStatus.AVBRUTT).build();
        mockHentAktivitet(aktivitet);

        testAlleOppdateringsmetoder(aktivitet);

    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_historisk() {
        val aktivitet = lagEnNyAktivitet().toBuilder().historiskDato(new Date()).build();
        mockHentAktivitet(aktivitet);

        testAlleOppdateringsmetoder(aktivitet);

    }

    private void testAlleOppdateringsmetoder(final AktivitetData aktivitet) {
        try {
            appService.oppdaterStatus(aktivitet);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            appService.oppdaterEtikett(aktivitet);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            appService.oppdaterReferat(aktivitet);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            appService.oppdaterAktivitet(aktivitet);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        verify(aktivitetService, never()).oppdaterStatus(any(), any(), any());
        verify(aktivitetService, never()).oppdaterAktivitet(any(), any(), any());
        verify(aktivitetService, never()).oppdaterAktivitetFrist(any(), any(), any());
        verify(aktivitetService, never()).oppdaterEtikett(any(), any(), any());
        verify(aktivitetService, never()).oppdaterMoteTidOgSted(any(), any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any(), any());

    }

    public void mockHentAktivitet(AktivitetData aktivitetData) {
        when(aktivitetService.hentAktivitet(AKTIVITET_ID)).thenReturn(aktivitetData);
    }


    public AktivitetData lagEnNyAktivitet() {
        val stilling = nyttStillingssøk();
        return nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .id(AKTIVITET_ID)
                .aktorId("haha")
                .stillingsSoekAktivitetData(stilling)
                .build();
    }

}