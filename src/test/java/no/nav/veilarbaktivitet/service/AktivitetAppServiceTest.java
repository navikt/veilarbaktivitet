package no.nav.veilarbaktivitet.service;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.BehandlingAktivitetData;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.person.Person;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.server.ResponseStatusException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static no.nav.veilarbaktivitet.person.InnsenderData.BRUKER;
import static no.nav.veilarbaktivitet.person.InnsenderData.NAV;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktivitetAppServiceTest {

    private final String AVTALT_BESKRIVELSE = "Avtalt beskrivelse";
    @Mock
    private AuthService authService;

    @Mock
    AktivitetService aktivitetService;

    @InjectMocks
    private AktivitetAppService appService;

    @Mock @SuppressWarnings("unused") // Blir faktisk brukt
    private MetricService metricService;

    @Test
    public void eksternbruker_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withId(AKTIVITET_ID).withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData oppdatertAktivitet = gammelAktivitet.withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(BRUKER);
        mockHentAktivitet(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureAktivitet.capture(), capturePerson.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(oppdatertAktivitet.getTilDato());
            Person endretAv = capturePerson.getValue();
            s.assertThat(endretAv.get()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(endretAv.tilBrukerType()).isSameAs(BRUKER);
            s.assertAll();
        });

    }

    @Test
    public void eksternbruker_skal_kunne_endre_flere_ting_nar_ikke_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet()
                .withAvtalt(false)
                .withBeskrivelse(AVTALT_BESKRIVELSE)
                .withBehandlingAktivitetData(avtaltBehandlingsdata)
                .withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData endretAktivitet = gammelAktivitet
                .withBeskrivelse("Endret beskrivelse")
                .withBehandlingAktivitetData(endretBehandlingsdata)
                .withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(endretAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(endretAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitet(any(), captureAktivitet.capture(), capturePerson.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(endretAktivitet.getTilDato());
            s.assertThat(aktivitet.getBeskrivelse()).isNotEqualTo(AVTALT_BESKRIVELSE);
            s.assertThat(aktivitet.getBeskrivelse()).isEqualTo(endretAktivitet.getBeskrivelse());
            s.assertThat(aktivitet.getBehandlingAktivitetData()).isNotEqualTo(avtaltBehandlingsdata);
            s.assertThat(aktivitet.getBehandlingAktivitetData()).isEqualTo(endretBehandlingsdata);
            Person endretAv = capturePerson.getValue();
            s.assertThat(endretAv.get()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(endretAv.tilBrukerType()).isSameAs(BRUKER);
            s.assertAll();
        });
    }

    @Test
    public void nav_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData oppdatertAktivitet = gammelAktivitet.withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureAktivitet.capture(), capturePerson.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(oppdatertAktivitet.getTilDato());
            Person endretAv = capturePerson.getValue();
            s.assertThat(endretAv.get()).isEqualTo(TESTNAVIDENT);
            s.assertThat(endretAv.tilBrukerType()).isSameAs(NAV);
            s.assertAll();
        });
    }

    @Test
    public void nav_skal_kunne_endre_noen_ting_selv_om_avtalt_mote() {
        AktivitetData gammelAktivitet = nyMoteAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2000-01-01"));
        AktivitetData oppdatertAktivitet = gammelAktivitet.withTilDato(toJavaUtilDate("2022-01-01"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any(), any());
    }

    @Test
    public void eksternbruker_skal_ikke_kunne_endre_andre_aktivitetstyper() {
        AktivitetData annenAktivitetstype = nyIJobbAktivitet().withAvtalt(true);
        AktivitetData endretAktivitet = annenAktivitetstype.withBeskrivelse("Endret beskrivelse");

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(annenAktivitetstype.getId())).thenReturn(annenAktivitetstype);

        Assertions.assertThatThrownBy(() -> appService.oppdaterAktivitet(endretAktivitet))
                .isInstanceOf(ResponseStatusException.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    public void ikke_innlogget_skal_ikke_kunne_endre_noe() {
        when(authService.erEksternBruker()).thenReturn(false);
        when(authService.erInternBruker()).thenReturn(false);
        AktivitetData aktivitet = nyBehandlingAktivitet();
        AktivitetData endretAktivitet = aktivitet
                .withBeskrivelse("Endret beskrivelse")
                .withBehandlingAktivitetData(endretBehandlingsdata);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(anyLong())).thenReturn(aktivitet);

        Assertions.assertThatThrownBy(() -> appService.oppdaterAktivitet(endretAktivitet))
                .isInstanceOf(ResponseStatusException.class)
                .hasStackTraceContaining("403 FORBIDDEN");
    }

    @Test(expected = ResponseStatusException.class)
    public void oppdaterAktivitet_aktivitetsTypeSomBrukerIkkeKanLage_kasterException() {
        var aktivitet = nyMoteAktivitet();
        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId())).thenReturn(aktivitet);

        appService.oppdaterAktivitet(aktivitet);
    }


    @Test
    public void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").status(AktivitetStatus.AVBRUTT).build();
        mockHentAktivitet(aktivitet);

        testAlleOppdateringsmetoderUnntattEtikett(aktivitet);
    }

    @Test
    public void skal_kunne_endre_etikett_nar_aktivitet_avbrutt_eller_fullfort() {
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").status(AktivitetStatus.AVBRUTT).build();
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(Person.aktorId("123")));
        mockHentAktivitet(aktivitet);
        AktivitetData aktivitetData = appService.oppdaterEtikett(aktivitet);
        Assertions.assertThat(aktivitetData).isNotNull();
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
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").historiskDato(new Date()).build();
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

    private void testAlleOppdateringsmetoderUnntattEtikett(final AktivitetData aktivitet) {
        try {
            appService.oppdaterStatus(aktivitet);
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


    private void loggetInnSom(InnsenderData innsenderData) {
        when(authService.erEksternBruker()).thenReturn(innsenderData == BRUKER);
        when(authService.erInternBruker()).thenReturn(innsenderData == NAV);
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(innsenderData == BRUKER ? Person.aktorId(TESTPERSONNUMMER) : Person.navIdent(TESTNAVIDENT)));
    }

    private static final long AKTIVITET_ID = 666L;
    private static final String KONTORSPERRE_ENHET_ID = "1224";
    private final String TESTPERSONNUMMER = "01010012345";
    private final ArgumentCaptor<AktivitetData> captureAktivitet = ArgumentCaptor.forClass(AktivitetData.class);
    private final ArgumentCaptor<Person> capturePerson = ArgumentCaptor.forClass(Person.class);
    private final String TESTNAVIDENT = "S314159";
    private final BehandlingAktivitetData endretBehandlingsdata = BehandlingAktivitetData.builder()
            .effekt("Endret effekt")
            .behandlingOppfolging("Endret behandlingOppfolging")
            .behandlingSted("Endret behandlingSted")
            .behandlingType("Endret behandlingType")
            .build();
    private final BehandlingAktivitetData avtaltBehandlingsdata = BehandlingAktivitetData.builder()
            .effekt("Avtalt")
            .behandlingOppfolging("Avtalt")
            .behandlingSted("Avtalt")
            .behandlingType("Avtalt")
            .build();

    public static Date toJavaUtilDate(String isoDato) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String datoMedKlokkeslett = isoDato+"T00:00:00.000+0200";
        try {
            return dateFormat.parse(datoMedKlokkeslett);
        } catch (ParseException e) {
            return null;
        }
    }
}
