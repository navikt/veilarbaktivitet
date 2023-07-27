package no.nav.veilarbaktivitet.service;

import lombok.val;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.BehandlingAktivitetData;
import no.nav.veilarbaktivitet.person.Innsender;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static no.nav.veilarbaktivitet.person.Innsender.BRUKER;
import static no.nav.veilarbaktivitet.person.Innsender.NAV;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AktivitetAppServiceTest {

    private final String AVTALT_BESKRIVELSE = "Avtalt beskrivelse";
    @Mock
    private IAuthService authService;

    @Mock
    AktivitetService aktivitetService;

    @InjectMocks
    private AktivitetAppService appService;

    @Mock
    @SuppressWarnings("unused") // Blir faktisk brukt
    private MetricService metricService;

    @Test
    void eksternbruker_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withId(AKTIVITET_ID).withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData oppdatertAktivitet = gammelAktivitet
                .withEndretAv(TESTPERSONNUMMER)
                .withEndretAvType(BRUKER)
                .withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureAktivitet.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(oppdatertAktivitet.getTilDato());
            s.assertThat(aktivitet.getEndretAv()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(aktivitet.getEndretAvType()).isSameAs(BRUKER);
            s.assertAll();
        });

    }

    @Test
    void eksternbruker_skal_kunne_endre_flere_ting_nar_ikke_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet()
                .withAvtalt(false)
                .withBeskrivelse(AVTALT_BESKRIVELSE)
                .withBehandlingAktivitetData(avtaltBehandlingsdata)
                .withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData endretAktivitet = gammelAktivitet
                .withEndretAv(TESTPERSONNUMMER)
                .withEndretAvType(BRUKER)
                .withBeskrivelse("Endret beskrivelse")
                .withBehandlingAktivitetData(endretBehandlingsdata)
                .withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(endretAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(endretAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitet(any(), captureAktivitet.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(endretAktivitet.getTilDato());
            s.assertThat(aktivitet.getBeskrivelse()).isNotEqualTo(AVTALT_BESKRIVELSE);
            s.assertThat(aktivitet.getBeskrivelse()).isEqualTo(endretAktivitet.getBeskrivelse());
            s.assertThat(aktivitet.getBehandlingAktivitetData()).isNotEqualTo(avtaltBehandlingsdata);
            s.assertThat(aktivitet.getBehandlingAktivitetData()).isEqualTo(endretBehandlingsdata);
            s.assertThat(aktivitet.getEndretAv()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(aktivitet.getEndretAvType()).isSameAs(BRUKER);
            s.assertAll();
        });
    }

    @Test
    void nav_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetData oppdatertAktivitet = gammelAktivitet
                .withEndretAv(TESTNAVIDENT)
                .withEndretAvType(NAV)
                .withTilDato(toJavaUtilDate("2022-12-12"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureAktivitet.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetData aktivitet = captureAktivitet.getValue();
            s.assertThat(aktivitet.getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(aktivitet.getTilDato()).isEqualTo(oppdatertAktivitet.getTilDato());
            s.assertThat(aktivitet.getEndretAv()).isEqualTo(TESTNAVIDENT);
            s.assertThat(aktivitet.getEndretAvType()).isSameAs(NAV);
            s.assertAll();
        });
    }

    @Test
    void nav_skal_kunne_endre_noen_ting_selv_om_avtalt_mote() {
        AktivitetData gammelAktivitet = nyMoteAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2000-01-01"));
        AktivitetData oppdatertAktivitet = gammelAktivitet.withTilDato(toJavaUtilDate("2022-01-01"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any());
    }

    @Test
    void eksternbruker_skal_ikke_kunne_endre_andre_aktivitetstyper() {
        AktivitetData annenAktivitetstype = nyIJobbAktivitet().withAvtalt(true);
        AktivitetData endretAktivitet = annenAktivitetstype.withBeskrivelse("Endret beskrivelse");

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(annenAktivitetstype.getId())).thenReturn(annenAktivitetstype);

        Assertions.assertThatThrownBy(() -> appService.oppdaterAktivitet(endretAktivitet))
                .isInstanceOf(ResponseStatusException.class)
                .hasStackTraceContaining("400 BAD_REQUEST");
    }

    @Test
    void ikke_innlogget_skal_ikke_kunne_endre_noe() {
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

    @Test
    void oppdaterAktivitet_aktivitetsTypeSomBrukerIkkeKanLage_kasterException() {
        var aktivitet = nyMoteAktivitet();
        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(aktivitet.getId()))
            .thenReturn(aktivitet);
        Assertions.assertThatThrownBy(() -> appService.oppdaterAktivitet(aktivitet))
            .isInstanceOf(ResponseStatusException.class);
    }


    @Test
    void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").status(AktivitetStatus.AVBRUTT).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
        testAlleOppdateringsmetoderUnntattEtikett(aktivitet);
    }

    @Test
    void skal_kunne_endre_etikett_nar_aktivitet_avbrutt_eller_fullfort() {
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").status(AktivitetStatus.AVBRUTT).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.oppdaterEtikett(aktivitet);
        Assertions.assertThat(aktivitetData).isNotNull();
    }

    @Test
    void opprett_skal_ikke_returnere_kontorsperreEnhet_naar_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID).withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);
        when(authService.erSystemBruker()).thenReturn(Boolean.TRUE);
        when(aktivitetService.opprettAktivitet(any())).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(aktivitet);
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isNull();
    }

    @Test
    void opprett_skal_returnere_kontorsperreEnhet_naar_ikke_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID).withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);
        when(authService.erSystemBruker()).thenReturn(Boolean.FALSE);
        when(aktivitetService.opprettAktivitet(any())).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(aktivitet);
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isEqualTo(KONTORSPERRE_ENHET_ID);
    }

    @Test
    void skal_ikke_kunne_endre_aktivitet_nar_den_er_historisk() {
        val aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId("haha").historiskDato(new Date()).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
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

        verify(aktivitetService, never()).oppdaterStatus(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitet(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, never()).oppdaterEtikett(any(), any());
        verify(aktivitetService, never()).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any());

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

        verify(aktivitetService, never()).oppdaterStatus(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitet(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, never()).oppdaterEtikett(any(), any());
        verify(aktivitetService, never()).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any());

    }


    private void loggetInnSom(Innsender innsender) {
        when(authService.erEksternBruker()).thenReturn(innsender == BRUKER);
        when(authService.erInternBruker()).thenReturn(innsender == NAV);
//        when(authService.getLoggedInnUser()).thenReturn(innsender == BRUKER
//                ? Person.aktorId(TESTPERSONNUMMER).eksternBrukerId()
//                : Person.navIdent(TESTNAVIDENT).otherNavIdent());
    }

    private static final long AKTIVITET_ID = 666L;
    private static final String KONTORSPERRE_ENHET_ID = "1224";
    private final String TESTPERSONNUMMER = "01010012345";
    private final ArgumentCaptor<AktivitetData> captureAktivitet = ArgumentCaptor.forClass(AktivitetData.class);
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
        String datoMedKlokkeslett = isoDato + "T00:00:00.000+0200";
        try {
            return dateFormat.parse(datoMedKlokkeslett);
        } catch (ParseException e) {
            return null;
        }
    }
}
