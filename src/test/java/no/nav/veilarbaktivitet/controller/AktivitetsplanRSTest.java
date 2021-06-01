package no.nav.veilarbaktivitet.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.val;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.arena.ArenaService;
import no.nav.veilarbaktivitet.avtaltMedNav.*;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mock.AuthContextRule;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.AktivitetService;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.MetricService;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.mock.TestData.*;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.nyttStillingssøk;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Aktivitetsplan interaksjoner der pålogget bruker er saksbehandler
 */
public class AktivitetsplanRSTest {

    private final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);


    private KvpClient kvpClient = mock(KvpClient.class);
    private KvpService kvpService = new KvpService(kvpClient);
    private MetricService metricService = mock(MetricService.class);
    private ForhaandsorienteringDAO fhoDao = mock(ForhaandsorienteringDAO.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private AvtaltMedNavService avtaltMedNavService = new AvtaltMedNavService(metricService, aktivitetDAO, fhoDao, meterRegistry);

    private AktivitetService aktivitetService = new AktivitetService(aktivitetDAO, avtaltMedNavService, kvpService, metricService);
    private AuthService authService = mock(AuthService.class);
    private ArenaService arenaService = mock(ArenaService.class);
    private AktivitetAppService appService = new AktivitetAppService(arenaService, authService, aktivitetService, metricService);

    private AktivitetsplanController aktivitetController = new AktivitetsplanController(authService, appService, mockHttpServletRequest);

    @Rule
    public AuthContextRule authContextRule = new AuthContextRule(AuthTestUtils.createAuthContext(UserRole.INTERN, KJENT_SAKSBEHANDLER.get()));

    @Before
    public void setup() {
        when(authService.getAktorIdForPersonBrukerService(any())).thenReturn(Optional.of(KJENT_AKTOR_ID));
        when(authService.getLoggedInnUser()).thenReturn(Optional.of(KJENT_SAKSBEHANDLER));
        when(authService.erInternBruker()).thenReturn(Boolean.TRUE);
        when(authService.erEksternBruker()).thenReturn(Boolean.FALSE);
        when(authService.sjekKvpTilgang(null)).thenReturn(true);
        mockHttpServletRequest.setParameter("fnr", KJENT_IDENT.get());
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }


    @Test
    public void hentAktivitetVersjoner_returnererForhaandsorienteringDataPaaKorrektInstans() {
        var id = new Random().nextLong();
        var aktivitet = nyttStillingssøk().withId(id).withAktorId(KJENT_AKTOR_ID.get());
        aktivitetDAO.insertAktivitet(aktivitet);

        aktivitetService.oppdaterStatus(aktivitet, aktivitet.withStatus(AktivitetStatus.GJENNOMFORES), KJENT_AKTOR_ID);
        var sisteAktivitetVersjon = aktivitetService.hentAktivitetMedForhaandsorientering(id);
        var fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build();
        avtaltMedNavService.opprettFHO(new AvtaltMedNavDTO().setAktivitetVersjon(sisteAktivitetVersjon.getVersjon()).setForhaandsorientering(fho), id, KJENT_AKTOR_ID, NavIdent.of("V123"));
        var resultat = aktivitetController.hentAktivitetVersjoner(String.valueOf(id));

        Assert.assertEquals(3, resultat.size());
        Assert.assertNull(resultat.get(0).getForhaandsorientering());
        Assert.assertNull(resultat.get(1).getForhaandsorientering());
        Assert.assertNull(resultat.get(2).getForhaandsorientering());

    }

    @Test
    public void hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter();
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void hent_aktivitet() {
        gitt_at_jeg_har_aktiviter();
        da_skal_jeg_kunne_hente_en_aktivitet();
    }

    @Test
    public void hent_aktivitetsplan_med_kontorsperre() {
        gitt_at_jeg_har_aktiviteter_med_kontorsperre();
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void hent_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_en_aktivitet_med_kontorsperre();
        da_skal_jeg_ikke_kunne_hente_noen_aktiviteter();
    }

    @Test
    public void opprett_aktivitet() {
        gitt_at_jeg_har_laget_en_aktivtet();
        nar_jeg_lagrer_aktivteten();
        da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan();
    }

    @Test
    public void oppdater_status() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        da_skal_min_aktivitet_fatt_ny_status();
    }

    @Test
    public void oppdater_etikett() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_etiketten_pa_en_aktivitet();
        da_skal_min_aktivitet_fatt_ny_etikett();
    }

    @Test
    public void hent_aktivitet_versjoner() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        nar_jeg_henter_versjoner_pa_denne_aktiviten();
        da_skal_jeg_fa_versjonene_pa_denne_aktiviteten();
    }

    @Test
    public void oppdater_aktivtet() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_en_av_aktiviten();
        da_skal_jeg_aktiviten_vare_endret();
    }

    @Test
    public void skal_ikke_kunne_endre_annet_enn_frist_pa_avtalte_aktiviter() {
        gitt_at_jeg_har_laget_en_aktivtet();
        gitt_at_jeg_har_satt_aktiviteten_til_avtalt();
        nar_jeg_lagrer_aktivteten();
        nar_jeg_oppdaterer_aktiviten();
        da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert();
    }



    private List<Long> lagredeAktivitetsIder;

    private List<AktivitetData> aktiviter = Arrays.asList(
            nyttStillingssøk(), nyttStillingssøk()
    );

    private AktivitetDTO aktivitet;

    private void gitt_at_jeg_har_aktiviter() {
        gitt_at_jeg_har_folgende_aktiviteter(aktiviter);
    }

    private void gitt_at_jeg_har_aktiviteter_med_kontorsperre() {
        gitt_at_jeg_har_folgende_aktiviteter(Arrays.asList(
                nyttStillingssøk(),
                nyttStillingssøk().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID),
                nyttStillingssøk(),
                nyttStillingssøk().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID)
        ));
    }

    private void gitt_at_jeg_har_en_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_folgende_aktiviteter(Collections.singletonList(
                nyttStillingssøk().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID)
        ));
    }

    private void gitt_at_jeg_har_folgende_aktiviteter(List<AktivitetData> aktiviteter) {
        lagredeAktivitetsIder = aktiviteter.stream()
                .map(aktivitet -> aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, KJENT_AKTOR_ID))
                .collect(Collectors.toList());
    }

    private void gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet();
    }

    private void gitt_at_jeg_har_satt_aktiviteten_til_avtalt() {
        aktivitet.setAvtalt(true);
    }

    private void nar_jeg_lagrer_aktivteten() {
        aktivitet = aktivitetController.opprettNyAktivitet(aktivitet, false);
    }

    private AktivitetDTO orignalAktivitet;

    private void nar_jeg_oppdaterer_aktiviten() {
        orignalAktivitet = nyAktivitet()
                .setAvtalt(true)
                .setOpprettetDato(aktivitet.getOpprettetDato())
                .setFraDato(aktivitet.getFraDato())
                .setId(aktivitet.getId());

        aktivitet = aktivitetController.oppdaterAktivitet(
                aktivitet.setBeskrivelse("noe tull")
                        .setArbeidsgiver("Justice league")
                        .setEtikett(EtikettTypeDTO.AVSLAG)
                        .setTilDato(new Date())
        );
    }

    private AktivitetStatus nyAktivitetStatus = AktivitetStatus.AVBRUTT;
    private EtikettTypeDTO nyAktivitetEtikett = EtikettTypeDTO.AVSLAG;

    private void nar_jeg_flytter_en_aktivitet_til_en_annen_status() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        this.aktivitet = aktivitetController.oppdaterStatus(aktivitet.setStatus(nyAktivitetStatus));
    }

    private void nar_jeg_oppdaterer_etiketten_pa_en_aktivitet() {
        val aktivitet = aktivitetController.hentAktivitetsplan().aktiviteter.get(0);
        this.aktivitet = aktivitetController.oppdaterEtikett(aktivitet.setEtikett(nyAktivitetEtikett));
    }

    private List<AktivitetDTO> versjoner;

    private void nar_jeg_henter_versjoner_pa_denne_aktiviten() {
        versjoner = aktivitetController.hentAktivitetVersjoner(aktivitet.getId());
    }

    private String nyLenke;
    private String nyAvsluttetKommentar;
    private Date oldOpprettetDato;

    private void nar_jeg_oppdaterer_en_av_aktiviten() {
        val originalAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(lagredeAktivitetsIder.get(0));
        oldOpprettetDato = originalAktivitet.getOpprettetDato();
        nyLenke = "itsOver9000.com";
        nyAvsluttetKommentar = "The more I talk, the more i understand why i'm single";

        val nyAktivitet = originalAktivitet
                .toBuilder()
                .lenke(nyLenke)
                .avsluttetKommentar(nyAvsluttetKommentar)
                .build();

        this.aktivitet = aktivitetController.oppdaterAktivitet(AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet, false));
        this.lagredeAktivitetsIder.set(0, Long.parseLong(this.aktivitet.getId()));
    }


    private void da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetController.hentAktivitetsplan().aktiviteter;
        assertThat(aktiviteter, hasSize(2));
    }

    private void da_skal_jeg_ikke_kunne_hente_noen_aktiviteter() {
        List<AktivitetDTO> aktiviteter = aktivitetController.hentAktivitetsplan().aktiviteter;
        assertThat(aktiviteter, hasSize(0));
    }

    private void da_skal_jeg_kunne_hente_en_aktivitet() {
        assertThat(lagredeAktivitetsIder.get(0).toString(),
                equalTo((aktivitetController.hentAktivitet(lagredeAktivitetsIder.get(0).toString())).getId()));
    }

    private void da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan() {
        assertThat(aktivitetService.hentAktiviteterForAktorId(KJENT_AKTOR_ID), hasSize(1));
    }

    private void da_skal_min_aktivitet_fatt_ny_status() {
        assertThat(aktivitet.getStatus(), equalTo(nyAktivitetStatus));
        assertThat(aktivitetService.hentAktivitetMedForhaandsorientering(Long.parseLong(aktivitet.getId())).getStatus(), equalTo(nyAktivitetStatus));
    }

    private void da_skal_min_aktivitet_fatt_ny_etikett() {
        assertThat(aktivitet.getEtikett(), equalTo(nyAktivitetEtikett));
    }

    private void da_skal_jeg_fa_versjonene_pa_denne_aktiviteten() {
        assertThat(versjoner, hasSize(2));
    }

    private void da_skal_jeg_aktiviten_vare_endret() {
        val lagretAktivitet = (AktivitetDTO)aktivitetController.hentAktivitet(this.lagredeAktivitetsIder.get(0).toString());
        assertThat(lagretAktivitet.getLenke(), equalTo(nyLenke));
        assertThat(lagretAktivitet.getAvsluttetKommentar(), equalTo(nyAvsluttetKommentar));
        assertThat(lagretAktivitet.getOpprettetDato(), equalTo(oldOpprettetDato));
    }

    private void da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert() {
        assertThat(aktivitet, equalTo(orignalAktivitet
                .setTilDato(aktivitet.getTilDato())
                .setVersjon(aktivitet.getVersjon()) //automatiske felter satt av systemet
                .setLagtInnAv(aktivitet.getLagtInnAv())
                .setTransaksjonsType(aktivitet.getTransaksjonsType())
                .setEndretDato(aktivitet.getEndretDato())
                .setEndretAv(AuthContextHolderThreadLocal.instance().getSubject().orElseThrow())
        ));
    }

    private AktivitetDTO nyAktivitet() {
        return new AktivitetDTO()
                .setTittel("tittel")
                .setBeskrivelse("beskr")
                .setLenke("lenke")
                .setType(AktivitetTypeDTO.STILLING)
                .setStatus(AktivitetStatus.GJENNOMFORES)
                .setFraDato(new Date())
                .setTilDato(new Date())
                .setKontaktperson("kontakt");
    }
}
