package no.nav.veilarbaktivitet.service;

import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.BehandlingAktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.MoteData;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvFerdigAktivitetException;
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvHistoriskAktivitetException;
import no.nav.veilarbaktivitet.eventsLogger.BigQueryClient;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
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
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.veilarbaktivitet.person.Innsender.BRUKER;
import static no.nav.veilarbaktivitet.person.Innsender.NAV;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class LestAktivitetAppServiceTest {

    private final String AVTALT_BESKRIVELSE = "Avtalt beskrivelse";
    @Mock
    private IAuthService authService;

    @Mock
    AktivitetService aktivitetService;

    @Mock
    PersonService personService;

    @InjectMocks
    private AktivitetAppService appService;

    @Mock
    BigQueryClient bigQueryClient;

    @Mock
    @SuppressWarnings("unused") // Blir faktisk brukt
    private MetricService metricService;

    @Test
    void eksternbruker_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withId(AKTIVITET_ID).withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetsEndring oppdatertAktivitet = toBehandlingEndring(
                gammelAktivitet
                        .withEndretAv(TESTPERSONNUMMER)
                        .withEndretAvType(BRUKER)
                        .withTilDato(toJavaUtilDate("2022-12-12"))
        );

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureEndring.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetsEndring endring = captureEndring.getValue();
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isEqualTo(toJavaUtilDate("2022-12-12"));
            s.assertThat(endring.getSporing().getEndretAv()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(endring.getSporing().getEndretAvType()).isSameAs(BRUKER);
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
        AktivitetsEndring endretAktivitet = toBehandlingEndring(
                gammelAktivitet
                        .withEndretAv(TESTPERSONNUMMER)
                        .withEndretAvType(BRUKER)
                        .withBeskrivelse("Endret beskrivelse")
                        .withBehandlingAktivitetData(endretBehandlingsdata)
                        .withTilDato(toJavaUtilDate("2022-12-12"))
        );

        loggetInnSom(BRUKER);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(endretAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(endretAktivitet);

        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitet(any(), captureEndring.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetsEndring endring = captureEndring.getValue();
            Behandling.Endre behandlingEndring = (Behandling.Endre) endring;
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isEqualTo(toJavaUtilDate("2022-12-12"));
            s.assertThat(endring.getMuterbareFelter().getBeskrivelse()).isNotEqualTo(AVTALT_BESKRIVELSE);
            s.assertThat(endring.getMuterbareFelter().getBeskrivelse()).isEqualTo("Endret beskrivelse");
            s.assertThat(behandlingEndring.getBehandlingAktivitetData()).isNotEqualTo(avtaltBehandlingsdata);
            s.assertThat(behandlingEndring.getBehandlingAktivitetData()).isEqualTo(endretBehandlingsdata);
            s.assertThat(endring.getSporing().getEndretAv()).isEqualTo(TESTPERSONNUMMER);
            s.assertThat(endring.getSporing().getEndretAvType()).isSameAs(BRUKER);
            s.assertAll();
        });
    }

    @Test
    void nav_skal_kunne_endre_sluttdato_selv_om_avtalt_medisinsk_behandling() {
        AktivitetData gammelAktivitet = nyBehandlingAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2022-12-10"));
        AktivitetsEndring oppdatertAktivitet = toBehandlingEndring(
                gammelAktivitet
                        .withEndretAv(TESTNAVIDENT)
                        .withEndretAvType(NAV)
                        .withTilDato(toJavaUtilDate("2022-12-12"))
        );

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(1))
                .oppdaterAktivitetFrist(any(), captureEndring.capture());

        SoftAssertions.assertSoftly(s -> {
            AktivitetsEndring endring = captureEndring.getValue();
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isNotEqualTo(gammelAktivitet.getTilDato());
            s.assertThat(endring.getMuterbareFelter().getTilDato()).isEqualTo(toJavaUtilDate("2022-12-12"));
            s.assertThat(endring.getSporing().getEndretAv()).isEqualTo(TESTNAVIDENT);
            s.assertThat(endring.getSporing().getEndretAvType()).isSameAs(NAV);
            s.assertAll();
        });
    }

    @Test
    void nav_skal_kunne_endre_noen_ting_selv_om_avtalt_mote() {
        AktivitetData gammelAktivitet = nyMoteAktivitet().withAvtalt(true).withTilDato(toJavaUtilDate("2000-01-01"));
        AktivitetsEndring oppdatertAktivitet = toMoteEndring(gammelAktivitet.withTilDato(toJavaUtilDate("2022-01-01")));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);
        verify(aktivitetService, times(1)).oppdaterAktivitet(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(0)).oppdaterMoteDetaljer(any(), any());
    }

    @Test
    void nav_skal_kun_oppdatere_tid_og_sted_naar_kun_tid_endres_paa_avtalt_mote() {
        MoteData originalMoteData = MoteData.builder()
                .adresse("Gammel adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withBeskrivelse("Original beskrivelse")
                .withFraDato(toJavaUtilDate("2022-01-01"))
                .withTilDato(toJavaUtilDate("2022-01-02"))
                .withMoteData(originalMoteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet
                .withFraDato(toJavaUtilDate("2022-02-01"))
                .withTilDato(toJavaUtilDate("2022-02-02"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(0)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_kun_oppdatere_tid_og_sted_naar_kun_sted_endres_paa_avtalt_mote() {
        MoteData originalMoteData = MoteData.builder()
                .adresse("Gammel adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Forberedelser")
                .build();
        MoteData nyMoteData = MoteData.builder()
                .adresse("Ny adresse")
                .kanal(KanalDTO.TELEFON)
                .forberedelser("Forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withMoteData(originalMoteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet.withMoteData(nyMoteData);

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(0)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_kun_oppdatere_detaljer_naar_kun_tittel_endres_paa_avtalt_mote() {
        MoteData moteData = MoteData.builder()
                .adresse("Adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withBeskrivelse("Original beskrivelse")
                .withMoteData(moteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet.withTittel("Ny tittel");

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_kun_oppdatere_detaljer_naar_kun_beskrivelse_endres_paa_avtalt_mote() {
        MoteData moteData = MoteData.builder()
                .adresse("Adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withBeskrivelse("Original beskrivelse")
                .withMoteData(moteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet.withBeskrivelse("Ny beskrivelse");

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_kun_oppdatere_detaljer_naar_kun_forberedelser_endres_paa_avtalt_mote() {
        MoteData originalMoteData = MoteData.builder()
                .adresse("Adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Original forberedelser")
                .build();
        MoteData nyMoteData = MoteData.builder()
                .adresse("Adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Nye forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withMoteData(originalMoteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet.withMoteData(nyMoteData);

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_oppdatere_baade_tid_sted_og_detaljer_naar_begge_endres_paa_avtalt_mote() {
        MoteData originalMoteData = MoteData.builder()
                .adresse("Gammel adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Original forberedelser")
                .build();
        MoteData nyMoteData = MoteData.builder()
                .adresse("Ny adresse")
                .kanal(KanalDTO.TELEFON)
                .forberedelser("Nye forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(true)
                .withTittel("Original tittel")
                .withBeskrivelse("Original beskrivelse")
                .withFraDato(toJavaUtilDate("2022-01-01"))
                .withMoteData(originalMoteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet
                .withTittel("Ny tittel")
                .withFraDato(toJavaUtilDate("2022-02-01"))
                .withMoteData(nyMoteData);

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(1)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(1)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(0)).oppdaterAktivitet(any(), any());
    }

    @Test
    void nav_skal_bruke_vanlig_oppdater_naar_mote_ikke_er_avtalt() {
        MoteData moteData = MoteData.builder()
                .adresse("Adresse")
                .kanal(KanalDTO.OPPMOTE)
                .forberedelser("Forberedelser")
                .build();
        AktivitetData gammelAktivitet = nyMoteAktivitet()
                .withAvtalt(false)
                .withTittel("Original tittel")
                .withMoteData(moteData);

        AktivitetData oppdatertAktivitet = gammelAktivitet
                .withTittel("Ny tittel")
                .withFraDato(toJavaUtilDate("2022-02-01"));

        loggetInnSom(NAV);
        when(aktivitetService.hentAktivitetMedForhaandsorientering(oppdatertAktivitet.getId())).thenReturn(gammelAktivitet);

        appService.oppdaterAktivitet(oppdatertAktivitet);

        verify(aktivitetService, times(0)).oppdaterMoteTidStedOgKanal(any(), any());
        verify(aktivitetService, times(0)).oppdaterMoteDetaljer(any(), any());
        verify(aktivitetService, times(1)).oppdaterAktivitet(any(), any());
    }

    @Test
    void eksternbruker_skal_ikke_kunne_endre_andre_aktivitetstyper() {
        AktivitetData annenAktivitetstype = nyIJobbAktivitet().withAvtalt(true);
        AktivitetsEndring endretAktivitet = toIjobbEndring(annenAktivitetstype.withBeskrivelse("Endret beskrivelse"));

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
        AktivitetsEndring endretAktivitet = toBehandlingEndring(
                aktivitet
                        .withBeskrivelse("Endret beskrivelse")
                        .withBehandlingAktivitetData(endretBehandlingsdata)
        );
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
        Assertions.assertThatThrownBy(() -> appService.oppdaterAktivitet(toMoteEndring(aktivitet)))
            .isInstanceOf(ResponseStatusException.class);
    }


    @Test
    void skal_ikke_kunne_endre_aktivitet_nar_den_er_avbrutt_eller_fullfort() {
        final var aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID).aktorId(Person.aktorId("haha")).status(AktivitetStatus.AVBRUTT).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
        testAlleOppdateringsmetoderUnntattEtikett(aktivitet);
    }

    @Test
    void skal_kunne_endre_etikett_nar_aktivitet_avbrutt_eller_fullfort() {
        final var aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID)
                .aktorId(Person.aktorId("haha")).status(AktivitetStatus.AVBRUTT).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.oppdaterEtikett(toEtikettEndring(aktivitet));
        Assertions.assertThat(aktivitetData).isNotNull();
    }

    @Test
    void opprett_skal_ikke_returnere_kontorsperreEnhet_naar_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID)
                .withOppfolgingsperiodeId(UUID.randomUUID())
                .withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);
        when(authService.erSystemBruker()).thenReturn(Boolean.TRUE);
        when(aktivitetService.opprettAktivitetIDB(any())).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(toMoteOpprettelse(aktivitet));
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isNull();
    }

    @Test
    void opprett_skal_returnere_kontorsperreEnhet_naar_ikke_systembruker() {
        var aktivitet = nyMoteAktivitet().withId(AKTIVITET_ID)
                .withOppfolgingsperiodeId(UUID.randomUUID())
                .withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);
        when(authService.erSystemBruker()).thenReturn(Boolean.FALSE);
        when(aktivitetService.opprettAktivitetIDB(any())).thenReturn(aktivitet);
        AktivitetData aktivitetData = appService.opprettNyAktivitet(toMoteOpprettelse(aktivitet));
        Assertions.assertThat(aktivitetData.getKontorsperreEnhetId()).isEqualTo(KONTORSPERRE_ENHET_ID);
    }

    @Test
    void skal_ikke_kunne_endre_aktivitet_nar_den_er_historisk() {
        final var aktivitet = nyttStillingssok().toBuilder().id(AKTIVITET_ID)
                .aktorId(Person.aktorId("haha")).historiskDato(new Date()).build();
        when(aktivitetService.hentAktivitetMedForhaandsorientering(AKTIVITET_ID)).thenReturn(aktivitet);
        testAlleOppdateringsmetoder(aktivitet);
    }

    @Test
    void skal_ikke_kaste_feil_selv_om_en_aktivitet_har_kontorsperre() {
        final var aktorId = Person.aktorId("haha");
        final var aktivitetMedKontorsperre = nyttStillingssok().toBuilder().id(AKTIVITET_ID)
                .aktorId(Person.aktorId("haha")).kontorsperreEnhetId("EnhetId").build();
        when(personService.getAktorIdForPersonBruker(any(Person.class))).thenReturn(Optional.of(aktorId));
        when(aktivitetService.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitetMedKontorsperre));

        assertDoesNotThrow(() -> {
            appService.hentAktiviteterUtenKontorsperre(aktorId);
        });
    }

    private void testAlleOppdateringsmetoder(final AktivitetData aktivitet) {
        try {
            appService.oppdaterStatus(toStatusEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }
        try {
            appService.oppdaterEtikett(toEtikettEndring(aktivitet));
            fail();
        } catch (EndringAvHistoriskAktivitetException ignored) {
        }
        try {
            appService.oppdaterReferat(toReferatEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }
        try {
            appService.oppdaterAktivitet(toJobbsoekingEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }

        verify(aktivitetService, never()).oppdaterStatus(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitet(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, never()).oppdaterEtikett(any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any());

    }

    private void testAlleOppdateringsmetoderUnntattEtikett(final AktivitetData aktivitet) {
        try {
            appService.oppdaterStatus(toStatusEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }
        try {
            appService.oppdaterReferat(toReferatEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }
        try {
            appService.oppdaterAktivitet(toJobbsoekingEndring(aktivitet));
            fail();
        } catch (EndringAvFerdigAktivitetException ignored) {
        }

        verify(aktivitetService, never()).oppdaterStatus(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitet(any(), any());
        verify(aktivitetService, never()).oppdaterAktivitetFrist(any(), any());
        verify(aktivitetService, never()).oppdaterEtikett(any(), any());
        verify(aktivitetService, never()).oppdaterReferat(any(), any());

    }


    private void loggetInnSom(Innsender innsender) {
        when(authService.erEksternBruker()).thenReturn(innsender == BRUKER);
        when(authService.erInternBruker()).thenReturn(innsender == NAV);
    }

    // --- Utility methods to convert AktivitetData to AktivitetsEndring ---

    private static SporingsData sporingsData(AktivitetData aktivitet) {
        return new SporingsData(
                aktivitet.getEndretAv() != null ? aktivitet.getEndretAv() : "unknown",
                aktivitet.getEndretAvType() != null ? aktivitet.getEndretAvType() : NAV,
                ZonedDateTime.now()
        );
    }

    private static AktivitetMuterbareFelter muterbareFelter(AktivitetData aktivitet) {
        return new AktivitetMuterbareFelter(
                aktivitet.getTittel(),
                aktivitet.getBeskrivelse(),
                aktivitet.getFraDato(),
                aktivitet.getTilDato(),
                aktivitet.getLenke()
        );
    }

    private static AktivitetBareOpprettFelter opprettFelter(AktivitetData aktivitet) {
        return new AktivitetBareOpprettFelter(
                aktivitet.getAktorId(),
                aktivitet.getAktivitetType(),
                aktivitet.getStatus(),
                aktivitet.getKontorsperreEnhetId(),
                aktivitet.getMalid(),
                ZonedDateTime.now(),
                aktivitet.isAutomatiskOpprettet(),
                aktivitet.getOppfolgingsperiodeId()
        );
    }

    private static Behandling.Endre toBehandlingEndring(AktivitetData aktivitet) {
        return new Behandling.Endre(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                muterbareFelter(aktivitet),
                sporingsData(aktivitet),
                aktivitet.getBehandlingAktivitetData()
        );
    }

    private static Mote.Endre toMoteEndring(AktivitetData aktivitet) {
        return new Mote.Endre(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                muterbareFelter(aktivitet),
                sporingsData(aktivitet),
                aktivitet.getMoteData()
        );
    }

    private static Mote.Opprett toMoteOpprettelse(AktivitetData aktivitet) {
        return new Mote.Opprett(
                opprettFelter(aktivitet),
                muterbareFelter(aktivitet),
                sporingsData(aktivitet),
                aktivitet.getMoteData()
        );
    }

    private static Ijobb.Endre toIjobbEndring(AktivitetData aktivitet) {
        return new Ijobb.Endre(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                muterbareFelter(aktivitet),
                sporingsData(aktivitet),
                aktivitet.getIJobbAktivitetData()
        );
    }

    private static Jobbsoeking.Endre toJobbsoekingEndring(AktivitetData aktivitet) {
        return new Jobbsoeking.Endre(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                muterbareFelter(aktivitet),
                sporingsData(aktivitet),
                aktivitet.getStillingsSoekAktivitetData()
        );
    }

    private static StatusEndring toStatusEndring(AktivitetData aktivitet) {
        return new StatusEndring(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                sporingsData(aktivitet),
                aktivitet.getStatus(),
                aktivitet.getAvsluttetKommentar()
        );
    }

    private static EtikettEndring toEtikettEndring(AktivitetData aktivitet) {
        return new EtikettEndring(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                sporingsData(aktivitet),
                aktivitet.getStillingsSoekAktivitetData() != null
                        ? aktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett()
                        : null
        );
    }

    private static ReferatEndring toReferatEndring(AktivitetData aktivitet) {
        return new ReferatEndring(
                aktivitet.getId(),
                aktivitet.getVersjon(),
                sporingsData(aktivitet),
                aktivitet.getMoteData() != null ? aktivitet.getMoteData() : MoteData.builder().build()
        );
    }

    private static final long AKTIVITET_ID = 666L;
    private static final String KONTORSPERRE_ENHET_ID = "1224";
    private final String TESTPERSONNUMMER = "01010012345";
    private final ArgumentCaptor<AktivitetsEndring> captureEndring = ArgumentCaptor.forClass(AktivitetsEndring.class);
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
