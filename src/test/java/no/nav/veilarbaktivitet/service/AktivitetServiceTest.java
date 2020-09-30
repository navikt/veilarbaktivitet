package no.nav.veilarbaktivitet.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.types.feil.VersjonsKonflikt;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.client.KvpClient;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.kafka.KafkaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DuplicateKeyException;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static no.nav.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.veilarbaktivitet.domain.AktivitetTypeData.MOTE;
import static no.nav.veilarbaktivitet.mock.TestData.KJENT_AKTOR_ID;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktivitetServiceTest {

    private static final long AKTIVITET_ID = 69L;
    private static final String KONTORSPERRE_ENHET_ID = "1337";

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private KvpClient kvpClient;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private FunksjonelleMetrikker funksjonelleMetrikker;

    @Captor
    private ArgumentCaptor argumentCaptor;

    @InjectMocks
    private AktivitetService aktivitetService;

    @Test
    public void opprettAktivitet() {
        val aktivitet = lagEnNyAktivitet();

        when(aktivitetDAO.getNextUniqueAktivitetId()).thenReturn(AKTIVITET_ID);
        when(kvpClient.get(KJENT_AKTOR_ID)).thenReturn(Optional.empty());
        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);

        captureInsertAktivitetArgument();

        assertThat(getCapturedAktivitet().getId(), equalTo(AKTIVITET_ID));
        assertThat(getCapturedAktivitet().getFraDato(), equalTo(aktivitet.getFraDato()));
        assertThat(getCapturedAktivitet().getTittel(), equalTo(aktivitet.getTittel()));

        assertThat(getCapturedAktivitet().getKontorsperreEnhetId(), nullValue());
        assertThat(getCapturedAktivitet().getAktorId(), equalTo(KJENT_AKTOR_ID.get()));
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.OPPRETTET));
        assertThat(getCapturedAktivitet().getOpprettetDato(), notNullValue());
    }

    @Test
    public void opprettAktivitetMedKvp() {
        val aktivitet = lagEnNyAktivitet();
        KvpDTO kvp = new KvpDTO().setEnhet(KONTORSPERRE_ENHET_ID);

        when(aktivitetDAO.getNextUniqueAktivitetId()).thenReturn(AKTIVITET_ID);
        when(kvpClient.get(KJENT_AKTOR_ID)).thenReturn(Optional.of(kvp));
        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);

        captureInsertAktivitetArgument();

        assertThat(getCapturedAktivitet().getKontorsperreEnhetId(), equalTo(kvp.getEnhet()));
    }

    @Test
    public void oppdaterStatus() {
        val aktivitet = lagEnNyAktivitet();

        val avsluttKommentar = "Alexander er best";
        val nyStatus = AktivitetStatus.GJENNOMFORES;
        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("ikke rett beskrivelse")
                .avsluttetKommentar(avsluttKommentar)
                .status(nyStatus)
                .build();
        aktivitetService.oppdaterStatus(aktivitet, oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getStatus(), equalTo(nyStatus));
        assertThat(getCapturedAktivitet().getAvsluttetKommentar(), equalTo(avsluttKommentar));
    }

    @SneakyThrows
    @Test
    public void oppdaterStatusMedKvpTilgang() {
        val aktivitet = lagEnNyAktivitet();
        val kvpAktivitet = aktivitet.withKontorsperreEnhetId(KONTORSPERRE_ENHET_ID);

        val nyStatus = AktivitetStatus.GJENNOMFORES;
        val oppdatertAktivitet = kvpAktivitet
                .toBuilder()
                .status(nyStatus)
                .build();

        aktivitetService.oppdaterStatus(kvpAktivitet, oppdatertAktivitet, null);
    }

    @Test
    public void oppdaterEtikett() {
        val aktivitet = lagEnNyAktivitet();

        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er fremdeles best")
                .stillingsSoekAktivitetData(aktivitet
                        .getStillingsSoekAktivitetData()
                        .withStillingsoekEtikett(StillingsoekEtikettData.AVSLAG))
                .build();
        aktivitetService.oppdaterEtikett(aktivitet, oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(aktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getStillingsSoekAktivitetData().getStillingsoekEtikett(),
                equalTo(StillingsoekEtikettData.AVSLAG));
    }

    @Test
    public void oppdaterAktivitetFrist() {
        val aktivitet = lagEnNyAktivitet();

        val nyFrist = ZonedDateTime.now();
        aktivitetService.oppdaterAktivitetFrist(aktivitet, aktivitet.toBuilder().tilDato(nyFrist).build(), null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTilDato(), equalTo(nyFrist));
    }

    @Test
    public void oppdaterMoteTidOgSted() {
        AktivitetData aktivitet = AktivitetDataTestBuilder.nyMoteAktivitet();

        val nyFrist = ZonedDateTime.now();
        String nyAdresse = "ny adresse";
        aktivitetService.oppdaterMoteTidStedOgKanal(aktivitet, aktivitet.withTilDato(nyFrist).withFraDato(nyFrist).withMoteData(aktivitet.getMoteData().withAdresse(nyAdresse)), null);

        captureInsertAktivitetArgument();
        AktivitetData capturedAktivitet = getCapturedAktivitet();

        assertThat(capturedAktivitet.getFraDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getTilDato(), equalTo(nyFrist));
        assertThat(capturedAktivitet.getMoteData().getAdresse(), equalTo(nyAdresse));
    }

    @Test
    public void oppdaterAktivitet() {
        val aktivitet = lagEnNyAktivitet();
        val oppdatertAktivitet = aktivitet
                .toBuilder()
                .beskrivelse("Alexander er den beste")
                .lenke("www.alexander-er-best.no")
                .build();

        aktivitetService.oppdaterAktivitet(aktivitet, oppdatertAktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getBeskrivelse(), equalTo(oppdatertAktivitet.getBeskrivelse()));
        assertThat(getCapturedAktivitet().getLenke(), equalTo(oppdatertAktivitet.getLenke()));
    }

    @Test(expected = VersjonsKonflikt.class)
    public void oppdaterAktivitet_skal_gi_versjonsKonflikt_hvis_to_oppdaterer_aktiviteten_samtidig() {
        val aktivitet = lagEnNyAktivitet();
        doThrow(new DuplicateKeyException("versjon fins")).when(aktivitetDAO).insertAktivitet(any());
        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet, null);
    }

    @Test
    public void oppdaterAktivitet_skal_sette_rett_transaksjonstype() {
        val aktivitet = lagEnNyAktivitet();

        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet, null);

        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.DETALJER_ENDRET));

        aktivitetService.oppdaterAktivitet(aktivitet, aktivitet.toBuilder().avtalt(true).build(), null);
        captureInsertAktivitetArgument();
        assertThat(getCapturedAktivitet().getTransaksjonsType(), equalTo(AktivitetTransaksjonsType.AVTALT));
    }

    @Test
    public void settAktiviteterTilHistoriske_ingenHistoriskDato_oppdaterAktivitet() {
        ZonedDateTime now = ZonedDateTime.now();
        gitt_aktivitet(lagEnNyAktivitet().withOpprettetDato(now.minusDays(1)));
        aktivitetService.settAktiviteterTilHistoriske(Person.aktorId("aktorId"), now);
        verify(aktivitetDAO).insertAktivitet(any());
    }

    @Test
    public void settAktiviteterTilHistoriske_harHistoriskDato_oppdaterIkkeAktivitet() {
        gitt_aktivitet(lagEnNyAktivitet().withHistoriskDato(ZonedDateTime.now().minusDays(5)));
        aktivitetService.settAktiviteterTilHistoriske(Person.aktorId("aktorId"), ZonedDateTime.now());
        verify(aktivitetDAO, never()).insertAktivitet(any());
    }

    @Test
    public void settAktiviteterTilHistoriske_opprettetEtterSluttDato_ikkeOppdaterAktivitet() {
        ZonedDateTime now = ZonedDateTime.now();
        gitt_aktivitet(lagEnNyAktivitet().withOpprettetDato(now.plusDays(1)));
        aktivitetService.settAktiviteterTilHistoriske(Person.aktorId("aktorId"), now);
        verify(aktivitetDAO, never()).insertAktivitet(any());
    }

    @Test
    public void settAktiviteterTilHistoriske_likHistoriskDato_ikkeOppdaterAktivitet() {
        ZonedDateTime now = ZonedDateTime.now();
        gitt_aktivitet(lagEnNyAktivitet().withHistoriskDato(now));
        aktivitetService.settAktiviteterTilHistoriske(Person.aktorId("aktorId"), now);
        verify(aktivitetDAO, never()).insertAktivitet(any());
    }

    @Test
    public void settLestAvBrukerTidspunkt_kaller_insertLestAvBrukerTidspunkt() {
        gitt_aktivitet(lagEnNyAktivitet());
        aktivitetService.settLestAvBrukerTidspunkt(AKTIVITET_ID);
        verify(aktivitetDAO, times(1)).insertLestAvBrukerTidspunkt(AKTIVITET_ID);
    }

    private void gitt_aktivitet(AktivitetData aktivitetData) {
        when(aktivitetDAO.hentAktiviteterForAktorId(any(Person.AktorId.class))).thenReturn(asList(aktivitetData));
    }

    public AktivitetData lagEnNyAktivitet() {
        return AktivitetDataTestBuilder.nyttStillingssøk();
    }

    public void captureInsertAktivitetArgument() {
        Mockito.verify(aktivitetDAO, atLeastOnce()).insertAktivitet((AktivitetData) argumentCaptor.capture());
    }

    public AktivitetData getCapturedAktivitet() {
        return ((AktivitetData) argumentCaptor.getValue());
    }

}
