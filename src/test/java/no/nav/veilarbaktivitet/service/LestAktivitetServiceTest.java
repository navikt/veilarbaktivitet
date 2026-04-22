package no.nav.veilarbaktivitet.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitet.domain.*;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.*;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.EtikettEndring;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring;
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService;
import no.nav.veilarbaktivitet.oversikten.OversiktenService;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testUtils.AktivitetDtoTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.util.DateUtils;
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

import static no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsOpprettelseUtil.tilAktivitetsData;
import static no.nav.veilarbaktivitet.mock.TestData.KJENT_KONTORSPERRE_ENHET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LestAktivitetServiceTest {

    private static final long AKTIVITET_ID = 69L;
    private static final long AKTIVITET_VERSJON = 1L;
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
        final var opprettDto = lagEnNyOpprettDto();
        final var expectedData = tilAktivitetsData(opprettDto);

        when(aktivitetDAO.opprettNyAktivitet(any(AktivitetData.class))).thenReturn(expectedData);
        aktivitetService.opprettAktivitetIDB(opprettDto);

        captureOpprettAktivitetArgument();

        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getFraDato(), equalTo(expectedData.getFraDato()));
        assertThat(capturedAktivitet.getTittel(), equalTo(expectedData.getTittel()));

        assertThat(capturedAktivitet.getKontorsperreEnhetId(), nullValue());
        assertNotNull(capturedAktivitet.getAktorId());
        assertThat(capturedAktivitet.getAktorId(), equalTo(expectedData.getAktorId()));
        assertThat(capturedAktivitet.getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.OPPRETTET));
        assertThat(capturedAktivitet.getOpprettetDato()).isEqualTo(expectedData.getOpprettetDato());
        assertThat(capturedAktivitet.getEndretDato()).isEqualTo(expectedData.getEndretDato());
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(expectedData.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
    }

    @Test
    void opprettAktivitetMedKvp() {
        final var opprettDto = lagEnNyOpprettDto(KJENT_KONTORSPERRE_ENHET_ID);
        final var expectedData = tilAktivitetsData(opprettDto);

        when(aktivitetDAO.opprettNyAktivitet(any(AktivitetData.class))).thenReturn(expectedData);
        aktivitetService.opprettAktivitetIDB(opprettDto);

        captureOpprettAktivitetArgument();

        assertThat(getCapturedAktivitet().getKontorsperreEnhetId(), equalTo(KONTORSPERRE_ENHET_ID));
    }

    @Test
    void oppdaterStatus() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto());

        final var avsluttKommentar = "Alexander er best";
        final var nyStatus = AktivitetStatus.GJENNOMFORES;
        final var sporingsData = new SporingsData("bruker", Innsender.BRUKER, ZonedDateTime.now());
        final var statusEndring = new StatusEndring(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                sporingsData,
                nyStatus,
                avsluttKommentar
        );
        aktivitetService.oppdaterStatus(aktivitet, statusEndring);

        captureOppdaterAktivitetWithDateArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(capturedAktivitet.getStatus(), equalTo(nyStatus));
        assertThat(capturedAktivitet.getAvsluttetKommentar(), equalTo(avsluttKommentar));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo("bruker"));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.BRUKER));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @SneakyThrows
    @Test
    void oppdaterStatusMedKvpTilgang() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto(KONTORSPERRE_ENHET_ID));

        final var nyStatus = AktivitetStatus.GJENNOMFORES;
        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var statusEndring = new StatusEndring(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                sporingsData,
                nyStatus,
                null
        );

        aktivitetService.oppdaterStatus(aktivitet, statusEndring);
        captureOppdaterAktivitetWithDateArgument();
        assertEquals(AktivitetStatus.GJENNOMFORES, getCapturedAktivitet().getStatus());
        assertThat(getCapturedAktivitet().getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @Test
    void oppdaterEtikett() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto());

        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var etikettEndring = new EtikettEndring(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                sporingsData,
                StillingsoekEtikettData.AVSLAG
        );
        aktivitetService.oppdaterEtikett(aktivitet, etikettEndring);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getStillingsSoekAktivitetData().getStillingsoekEtikett(),
                equalTo(StillingsoekEtikettData.AVSLAG));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @Test
    void oppdaterReferat() {
        final var aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        String REFERAT = "Referat";

        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var referatEndring = new ReferatEndring(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                sporingsData,
                MoteData.builder()
                        .referat(REFERAT)
                        .build()
        );
        aktivitetService.oppdaterReferat(aktivitet, referatEndring);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getMoteData().getReferat(),
                equalTo(REFERAT));
        assertThat(capturedAktivitet.getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.REFERAT_ENDRET));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @Test
    void oppdaterAktivitetFrist() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto());

        final var nyFrist = new Date();
        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var endring = new Jobbsoeking.Endre(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                new AktivitetMuterbareFelter(
                        aktivitet.getTittel(),
                        aktivitet.getBeskrivelse(),
                        aktivitet.getFraDato(),
                        nyFrist,
                        aktivitet.getLenke()
                ),
                sporingsData,
                aktivitet.getStillingsSoekAktivitetData()
        );
        aktivitetService.oppdaterAktivitetFrist(aktivitet, endring);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getTilDato(), equalTo(nyFrist));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @Test
    void oppdaterMoteTidOgSted() {
        AktivitetData aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        Date nyFrist = new Date();
        String nyAdresse = "ny adresse";
        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var endring = new Mote.Endre(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                new AktivitetMuterbareFelter(
                        aktivitet.getTittel(),
                        aktivitet.getBeskrivelse(),
                        nyFrist,
                        nyFrist,
                        aktivitet.getLenke()
                ),
                sporingsData,
                aktivitet.getMoteData().withAdresse(nyAdresse)
        );
        aktivitetService.oppdaterAktivitet(aktivitet, endring);

        captureOppdaterAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();

        assertThat(capturedAktivitet.getFraDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getTilDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getMoteData().getAdresse(), equalTo(nyAdresse));
        assertNotNull(capturedAktivitet.getEndretAv());
        assertThat(capturedAktivitet.getEndretAv(), equalTo(aktivitet.getEndretAv()));
        assertThat(capturedAktivitet.getEndretAvType(), equalTo(Innsender.NAV));
        assertNotNull(capturedAktivitet.getEndretDato());
        assertThat(capturedAktivitet.getEndretDato()).isCloseTo(DateUtils.zonedDateTimeToDate(sporingsData.getEndretDato()), 1);
    }

    @Test
    void oppdaterAktivitet() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto());
        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var endring = new Jobbsoeking.Endre(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                new AktivitetMuterbareFelter(
                        aktivitet.getTittel(),
                        "Alexander er den beste",
                        aktivitet.getFraDato(),
                        aktivitet.getTilDato(),
                        "www.alexander-er-best.no"
                ),
                sporingsData,
                aktivitet.getStillingsSoekAktivitetData()
        );

        aktivitetService.oppdaterAktivitet(aktivitet, endring);

        captureOppdaterAktivitetWithDateArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();
        assertThat(capturedAktivitet.getBeskrivelse(), equalTo("Alexander er den beste"));
        assertThat(capturedAktivitet.getLenke(), equalTo("www.alexander-er-best.no"));
    }

    @Disabled("Må fikses")
    @Test
    void oppdaterAktivitet_skal_gi_versjonsKonflikt_hvis_to_oppdaterer_aktiviteten_samtidig() {
        final var aktivitet = tilAktivitetsData(lagEnNyOpprettDto());
        doThrow(new DuplicateKeyException("versjon fins")).when(aktivitetDAO).oppdaterAktivitet(any());
        final var sporingsData = new SporingsData(aktivitet.getEndretAv(), Innsender.NAV, ZonedDateTime.now());
        final var endring = new Jobbsoeking.Endre(
                AKTIVITET_ID,
                AKTIVITET_VERSJON,
                new AktivitetMuterbareFelter(
                        aktivitet.getTittel(),
                        aktivitet.getBeskrivelse(),
                        aktivitet.getFraDato(),
                        aktivitet.getTilDato(),
                        aktivitet.getLenke()
                ),
                sporingsData,
                aktivitet.getStillingsSoekAktivitetData()
        );

        try {
            aktivitetService.oppdaterAktivitet(aktivitet, endring);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        }
    }

    @Test
    void settLestAvBrukerTidspunkt_kaller_insertLestAvBrukerTidspunkt() {
        gitt_aktivitet(tilAktivitetsData(lagEnNyOpprettDto()).withId(AKTIVITET_ID));
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

        final var sporingsData = new SporingsData("system", Innsender.SYSTEM, ZonedDateTime.now());
        final var statusEndring = new StatusEndring(
                aktivitetId,
                0L,
                sporingsData,
                AktivitetStatus.AVBRUTT,
                null
        );

        aktivitetService.oppdaterStatus(aktivitet, statusEndring);

        verify(oversiktenService).lagreStoppMeldingOmUdeltSamtalereferatIUtboks(aktorId, aktivitetId);
        verify(aktivitetDAO).oppdaterAktivitet(any());
    }

    private void gitt_aktivitet(AktivitetData aktivitetData) {
        when(aktivitetDAO.hentAktivitet(aktivitetData.getId())).thenReturn(aktivitetData);
    }

    public Jobbsoeking.Opprett lagEnNyOpprettDto() {
        return AktivitetDtoTestBuilder.nyttStillingssok(null, null, null);
    }

    public Jobbsoeking.Opprett lagEnNyOpprettDto(String kontorsperreEnhetId) {
        var base = lagEnNyOpprettDto();
        var opprettFelter = base.getOpprettFelter();
        var medKvp = new AktivitetBareOpprettFelter(
                opprettFelter.getAktorId(),
                opprettFelter.getAktivitetType(),
                opprettFelter.getStatus(),
                kontorsperreEnhetId,
                opprettFelter.getMalid(),
                opprettFelter.getOpprettetDato(),
                opprettFelter.getAutomatiskOpprettet(),
                opprettFelter.getOppfolgingsperiodeId()
        );
        return new Jobbsoeking.Opprett(medKvp, base.getMuterbareFelter(), base.getSporing(), base.getStillingsSoekAktivitetData());
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
