package no.nav.veilarbaktivitet.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Date;

import static no.nav.veilarbaktivitet.mock.TestData.KJENT_KONTORSPERRE_ENHET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.DateUtil.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AktivitetServiceTest {

    private static final long AKTIVITET_ID = 69L;
    private static final String KONTORSPERRE_ENHET_ID = "1337";

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private MetricService metricService;

    @Mock
    private AvtaltMedNavService avtaltMedNavService;

    @Mock
    private SistePeriodeService sistePeriodeService;

    @Mock
    private OversiktenService oversiktenService;

    @Captor
    private ArgumentCaptor<AktivitetData> argumentCaptor;

    private AktivitetService aktivitetService;

    @BeforeEach
    public void setup() {
        aktivitetService = new AktivitetService(aktivitetDAO, avtaltMedNavService, metricService, sistePeriodeService, oversiktenService);
    }

    @Test
    void viktigeFelterSkalPropageresTilDaoVedOpprettAktivitet() {
        final var aktivitet = lagEnNyAktivitet();

        when(aktivitetDAO.opprettNyAktivitet(any(AktivitetData.class))).thenReturn(aktivitet);
        aktivitetService.opprettAktivitet( aktivitet);

        captureOpprettAktivitetArgument();

        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getFraDato(), equalTo(aktivitet.getFraDato()));
        assertThat(capturedAktivitet.getTittel(), equalTo(aktivitet.getTittel()));

        assertThat(capturedAktivitet.getKontorsperreEnhetId(), nullValue());
        assertNotNull(capturedAktivitet.getAktorId());
        assertThat(capturedAktivitet.getAktorId(), equalTo(aktivitet.getAktorId()));
        assertThat(capturedAktivitet.getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.OPPRETTET));
        assertThat(capturedAktivitet.getOpprettetDato()).isEqualTo(aktivitet.getOpprettetDato());
        assertThat(capturedAktivitet.getEndretDato()).isEqualTo(aktivitet.getEndretDato());
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
    }

    @Test
    void opprettAktivitetMedKvp() {
        final var aktivitet = lagEnNyAktivitet().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID);

        when(aktivitetDAO.opprettNyAktivitet(any(AktivitetData.class))).thenReturn(aktivitet);
        aktivitetService.opprettAktivitet(aktivitet);

        captureOpprettAktivitetArgument();

        assertThat(getCapturedAktivitet().getKontorsperreEnhetId(), equalTo(KONTORSPERRE_ENHET_ID));
    }

    @Test
    void oppdaterStatus() {
        final var aktivitet = lagEnNyAktivitet();

        final var avsluttKommentar = "Alexander er best";
        final var nyStatus = AktivitetStatus.GJENNOMFORES;
        final var oppdatertAktivitet = aktivitet
                .toBuilder()
                .endretAv("bruker")
                .endretAvType(Innsender.BRUKER)
                .endretDato(new Date())
                .beskrivelse("ikke rett beskrivelse")
                .avsluttetKommentar(avsluttKommentar)
                .status(nyStatus)
                .build();
        aktivitetService.oppdaterStatus(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetWithDateArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(capturedAktivitet.getStatus(), equalTo(nyStatus));
        assertThat(capturedAktivitet.getAvsluttetKommentar(), equalTo(avsluttKommentar));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(oppdatertAktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(oppdatertAktivitet.getEndretAvType()));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(oppdatertAktivitet.getEndretDato(), 1);
    }

    @SneakyThrows
    @Test
    void oppdaterStatusMedKvpTilgang() {
        final var aktivitet = lagEnNyAktivitet();
        final var kvpAktivitet = aktivitet.withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);

        final var nyStatus = AktivitetStatus.GJENNOMFORES;
        final var oppdatertAktivitet = kvpAktivitet
                .toBuilder()
                .endretDato(new Date())
                .status(nyStatus)
                .build();

        aktivitetService.oppdaterStatus(kvpAktivitet, oppdatertAktivitet);
        captureOppdaterAktivitetWithDateArgument();
        assertEquals(AktivitetStatus.GJENNOMFORES, getCapturedAktivitet().getStatus());
        assertThat(getCapturedAktivitet().getEndretDato()).isCloseTo(oppdatertAktivitet.getEndretDato(), 1);
    }

    @Test
    void oppdaterEtikett() {
        final var aktivitet = lagEnNyAktivitet();

        final var oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er fremdeles best")
                .endretDato(new Date())
                .stillingsSoekAktivitetData(aktivitet
                        .getStillingsSoekAktivitetData()
                        .withStillingsoekEtikett(StillingsoekEtikettData.AVSLAG))
                .build();
        aktivitetService.oppdaterEtikett(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett(),
                equalTo(StillingsoekEtikettData.AVSLAG));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(oppdatertAktivitet.getEndretDato(), 1);
    }

    @Test
    void oppdaterReferat() {
        final var aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        String REFERAT = "Referat";

        final var oppdatertAktivitet = aktivitet
                .toBuilder()
                .endretDato(new Date())
                .beskrivelse("Alexander er fremdeles best")
                .moteData(MoteData.builder()
                        .referat(REFERAT)
                        .build())
                .build();
        aktivitetService.oppdaterReferat(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getMoteData().getReferat(),
                equalTo(REFERAT));
        assertThat(capturedAktivitet.getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.REFERAT_ENDRET));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(oppdatertAktivitet.getEndretDato(), 1);
    }

    @Test
    void oppdaterAktivitetFrist() {
        final var aktivitet = lagEnNyAktivitet();

        final var nyFrist = new Date();
        final var oppdatertAktivitet = aktivitet.toBuilder().endretDato(new Date()).tilDato(nyFrist).build();
        aktivitetService.oppdaterAktivitetFrist(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getTilDato(), equalTo(nyFrist));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(oppdatertAktivitet.getEndretDato(), 1);
    }

    @Test
    void oppdaterMoteTidOgSted() {
        AktivitetData aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        Date nyFrist = new Date();
        String nyAdresse = "ny adresse";
        var oppdatertAktivitet = aktivitet
                .withEndretDato(new Date())
                .withTilDato(nyFrist).withFraDato(nyFrist).withMoteData(aktivitet.getMoteData().withAdresse(nyAdresse));
        aktivitetService.oppdaterMoteTidStedOgKanal(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();

        assertThat(capturedAktivitet.getFraDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getTilDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getMoteData().getAdresse(), equalTo(nyAdresse));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertNotNull(capturedAktivitet.getEndretDato());
        assertThat(capturedAktivitet.getEndretDato()).isEqualTo(oppdatertAktivitet.getEndretDato());
    }

    @Test
    void oppdaterAktivitet() {
        final var aktivitet = lagEnNyAktivitet();
        final var oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er den beste")
                .lenke("www.alexander-er-best.no")
                .build();

        aktivitetService.oppdaterAktivitet(aktivitet, oppdatertAktivitet);

        captureOppdaterAktivitetWithDateArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(oppdatertAktivitet.getBeskrivelse()));
        assertThat(capturedAktivitet.getLenke(), equalTo(oppdatertAktivitet.getLenke()));
    }

    @Disabled("Må fikses")
    @Test
    void oppdaterAktivitet_skal_gi_versjonsKonflikt_hvis_to_oppdaterer_aktiviteten_samtidig() {
        final var aktivitet = lagEnNyAktivitet();
        doThrow(new DuplicateKeyException("versjon fins")).when(aktivitetDAO).oppdaterAktivitet(any());

        try {
            aktivitetService.oppdaterAktivitet(aktivitet, aktivitet);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        }
    }

    @Test
    void settLestAvBrukerTidspunkt_kaller_insertLestAvBrukerTidspunkt() {
        gitt_aktivitet(lagEnNyAktivitet().withId(AKTIVITET_ID));
        aktivitetService.settLestAvBrukerTidspunkt(AKTIVITET_ID);
        verify(aktivitetDAO, times(1)).insertLestAvBrukerTidspunkt(AKTIVITET_ID);
    }

    @ParameterizedTest
    @EnumSource(value = AktivitetTypeData.class, names = {"MOTE", "SAMTALEREFERAT"})
    void skal_lagre_stopp_melding_for_avbrutt_mote_og_samtalereferat(AktivitetTypeData aktivitetType) {
        val aktorId = new Person.AktorId("1231231231230");
        val aktivitetId = 123456789L;
        AktivitetData aktivitet = AktivitetData.builder()
                .id(aktivitetId)
                .aktorId(aktorId)
                .opprettetDato(Date.from(ZonedDateTime.now().minusDays(7).toInstant()))
                .aktivitetType(aktivitetType)
                .status(AktivitetStatus.PLANLAGT)
                .kontorsperreEnhetId("9001")
                .build();

        //when(aktivitetDAO.hentAktiviteterForAktorId(aktorId)).thenReturn(List.of(aktivitet));

        aktivitetService.oppdaterStatus(aktivitet, aktivitet.withStatus(AktivitetStatus.AVBRUTT).withEndretDato(now()));

        verify(oversiktenService).lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitetId);
        verify(aktivitetDAO).oppdaterAktivitet(any());
    }

    private void gitt_aktivitet(AktivitetData aktivitetData) {
        when(aktivitetDAO.hentAktivitet(aktivitetData.getId())).thenReturn(aktivitetData);
    }

    public AktivitetData lagEnNyAktivitet() {
        return AktivitetDataTestBuilder.nyttStillingssok()
                .withEndretDato(LocalDateTime.now().minusSeconds(1).toDate());
    }

    public void captureOppdaterAktivitetArgument() {
        Mockito.verify(aktivitetDAO, atLeastOnce()).oppdaterAktivitet(argumentCaptor.capture());
    }

    public void captureOppdaterAktivitetWithDateArgument() {
        Mockito.verify(aktivitetDAO, atLeastOnce()).oppdaterAktivitet(argumentCaptor.capture());
    }

    public void captureOpprettAktivitetArgument() {
        Mockito.verify(aktivitetDAO, atLeastOnce()).opprettNyAktivitet((argumentCaptor.capture()));
    }

    public AktivitetData getCapturedAktivitet() {
        return (argumentCaptor.getValue());
    }

}
