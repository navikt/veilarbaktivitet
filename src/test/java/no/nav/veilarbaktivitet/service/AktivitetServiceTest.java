package no.nav.veilarbaktivitet.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.kvp.v2.KvpV2DTO;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static no.nav.veilarbaktivitet.mock.TestData.KJENT_KONTORSPERRE_ENHET_ID;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Captor
    private ArgumentCaptor<AktivitetData> argumentCaptor;

    private AktivitetService aktivitetService;

    @BeforeEach
    public void setup() {
        aktivitetService = new AktivitetService(aktivitetDAO, avtaltMedNavService, metricService, sistePeriodeService);
    }

    @Test
    void viktigeFelterSkalPropageresTilDaoVedOpprettAktivitet() {
        val aktivitet = lagEnNyAktivitet();

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
        val aktivitet = lagEnNyAktivitet().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID);
        KvpV2DTO kvp = new KvpV2DTO().setEnhet(KONTORSPERRE_ENHET_ID);

        when(aktivitetDAO.opprettNyAktivitet(any(AktivitetData.class))).thenReturn(aktivitet);
        aktivitetService.opprettAktivitet(aktivitet);

        captureOpprettAktivitetArgument();

        assertThat(getCapturedAktivitet().getKontorsperreEnhetId(), equalTo(kvp.getEnhet()));
    }

    @Test
    void oppdaterStatus() {
        val aktivitet = lagEnNyAktivitet();

        val avsluttKommentar = "Alexander er best";
        val nyStatus = AktivitetStatus.GJENNOMFORES;
        val oppdatertAktivitet = aktivitet
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
        val aktivitet = lagEnNyAktivitet();
        val kvpAktivitet = aktivitet.withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);

        val nyStatus = AktivitetStatus.GJENNOMFORES;
        val oppdatertAktivitet = kvpAktivitet
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
        val aktivitet = lagEnNyAktivitet();

        val oppdatertAktivitet = aktivitet
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
        val aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        String REFERAT = "Referat";

        val oppdatertAktivitet = aktivitet
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
        val aktivitet = lagEnNyAktivitet();

        val nyFrist = new Date();
        val oppdatertAktivitet = aktivitet.toBuilder().endretDato(new Date()).tilDato(nyFrist).build();
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
        val aktivitet = lagEnNyAktivitet();
        val oppdatertAktivitet = aktivitet
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
        val aktivitet = lagEnNyAktivitet();
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
