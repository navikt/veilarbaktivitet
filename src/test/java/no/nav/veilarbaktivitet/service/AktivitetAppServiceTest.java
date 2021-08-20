package no.nav.veilarbaktivitet.service;

import lombok.val;
import no.nav.veilarbaktivitet.avtaltMedNav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.Person;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.nyMoteAktivitet;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.nyttStillingssøk;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetAppServiceTest {
    private static final long AKTIVITET_ID = 666L;
    public static final String KONTORSPERRE_ENHET_ID = "1224";

    @Mock
    private AuthService authService;

    @Mock
    private AktivitetService aktivitetService;

    @Mock
    private MetricService metricService;

    @Mock
    private AvtaltMedNavService avtaltMedNavService;

    @InjectMocks
    private AktivitetAppService appService;

    @Test(expected = ResponseStatusException.class)
    public void oppdaterAktivitet_aktivitetsTypeSomBrukerIkkeKanLage_kasterException(){
        var aktivitet = nyMoteAktivitet();
        when(authService.erEksternBruker()).thenReturn(true);
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(Person.aktorId("123")));
        when(aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId())).thenReturn(aktivitet);

        appService.oppdaterAktivitet(aktivitet);
    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitet = nyttStillingssøk().toBuilder().id(AKTIVITET_ID).aktorId("haha").status(AktivitetStatus.AVBRUTT).build();
        mockHentAktivitet(aktivitet);

        testAlleOppdateringsmetoder(aktivitet);
    }

    @Test
    public void opprett_skal_ikke_returnere_kontorsperreEnhet_naar_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID).withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);

        when(authService.getLoggedInnUser()).thenReturn(Optional.of(Person.navIdent("systembruker")));
        when(authService.getAktorIdForPersonBrukerService(Person.fnr("123"))).thenReturn(Optional.of(Person.aktorId("321")));
        when(authService.erSystemBruker()).thenReturn(Boolean.TRUE);
        when(aktivitetService.opprettAktivitet(any(), any(), any())).thenReturn(aktivitet);

        mockHentAktivitet(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(Person.fnr("123"), aktivitet);
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isNull();
    }

    @Test
    public void opprett_skal_returnere_kontorsperreEnhet_naar_ikke_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID).withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);

        when(authService.getLoggedInnUser()).thenReturn(Optional.of(Person.navIdent("saksbehandler")));
        when(authService.getAktorIdForPersonBrukerService(Person.fnr("123"))).thenReturn(Optional.of(Person.aktorId("321")));
        when(authService.erSystemBruker()).thenReturn(Boolean.FALSE);
        when(aktivitetService.opprettAktivitet(any(), any(), any())).thenReturn(aktivitet);

        mockHentAktivitet(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(Person.fnr("123"), aktivitet);
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isEqualTo(KONTORSPERRE_ENHET_ID);
    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_historisk() {
        val aktivitet = nyttStillingssøk().toBuilder().id(AKTIVITET_ID).aktorId("haha").historiskDato(new Date()).build();
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
        verify(aktivitetService, never()).oppdaterMoteTidStedOgKanal(any(), any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any(), any());

    }

    public void mockHentAktivitet(AktivitetData aktivitetData) {
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitetData);
    }

}
