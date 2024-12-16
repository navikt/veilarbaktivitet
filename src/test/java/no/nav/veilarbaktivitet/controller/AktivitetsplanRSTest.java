package no.nav.veilarbaktivitet.controller;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Type;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Aktivitetsplan interaksjoner der pålogget bruker er saksbehandler
 */

class AktivitetsplanRSTest extends SpringBootTestBase {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AktivitetDAO aktivitetDAO;

    @Autowired
    private AktivitetService aktivitetService;

    @Autowired
    private AvtaltMedNavService avtaltMedNavService;

    private AktivitetDTO orignalAktivitet;
    private final AktivitetStatus nyAktivitetStatus = AktivitetStatus.AVBRUTT;
    private final EtikettTypeDTO nyAktivitetEtikett = EtikettTypeDTO.AVSLAG;
    private List<Long> lagredeAktivitetsIder;

    private AktivitetDTO aktivitet;
    private MockBruker mockBruker;
    private MockVeileder mockBrukersVeileder;
    private MockVeileder annenMockVeilederMedNasjonalTilgang;
    private MockVeileder aktivVeileder;



    @BeforeEach
    void moreSettup() {
        mockBruker = navMockService.createBruker(BrukerOptions.happyBruker());
        mockBrukersVeileder = navMockService.createVeileder(mockBruker);
        annenMockVeilederMedNasjonalTilgang = navMockService.createVeilederMedNasjonalTilgang();
        aktivVeileder = mockBrukersVeileder;
    }

    @AfterEach
    void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }


    @Test
    void hentAktivitetVersjoner_returnererIkkeForhaandsorientering() {
        var aktivitet = aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()));
        Long aktivitetId = aktivitet.getId();
        aktivitetService.oppdaterStatus(aktivitet, aktivitet.withStatus(AktivitetStatus.GJENNOMFORES));
        var sisteAktivitetVersjon = aktivitetService.hentAktivitetMedForhaandsorientering(aktivitetId);
        var fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build();
        avtaltMedNavService.opprettFHO(new AvtaltMedNavDTO().setAktivitetVersjon(sisteAktivitetVersjon.getVersjon()).setForhaandsorientering(fho), aktivitetId, mockBruker.getAktorId(), NavIdent.of("V123"));
        var resultat = aktivitetTestService.hentVersjoner(String.valueOf(aktivitetId), mockBruker, mockBrukersVeileder);


        assertEquals(3, resultat.size());
        assertEquals(AktivitetTransaksjonsType.AVTALT, resultat.get(0).getTransaksjonsType());
        assertNull(resultat.get(0).getForhaandsorientering());
        assertNull(resultat.get(1).getForhaandsorientering());
        assertNull(resultat.get(2).getForhaandsorientering());
    }

    @Test
    void hentAktivitetsplan_henterAktiviteterMedForhaandsorientering() {
        AktivitetData aktivitetDataMedForhaandsorientering = aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()))
                .withOppfolgingsperiodeId(mockBruker.getOppfolgingsperiodeId());
        AktivitetData aktivitetDataUtenForhaandsorientering = aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()))
                .withOppfolgingsperiodeId(mockBruker.getOppfolgingsperiodeId());

        var fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build();
        avtaltMedNavService.opprettFHO(new AvtaltMedNavDTO().setAktivitetVersjon(aktivitetDataMedForhaandsorientering.getVersjon()).setForhaandsorientering(fho), aktivitetDataMedForhaandsorientering.getId(), mockBruker.getAktorId(), NavIdent.of("V123"));
        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder);
        assertNotNull(resultat.getAktiviteter().stream().filter(aktivitet -> Objects.equals(aktivitet.getId(), aktivitetDataMedForhaandsorientering.getId().toString())).toList().getFirst().getForhaandsorientering());
        assertNull(resultat.getAktiviteter().stream().filter(aktivitet -> Objects.equals(aktivitet.getId(), aktivitetDataUtenForhaandsorientering.getId().toString())).toList().getFirst().getForhaandsorientering());
    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataUtenCVData() {
        var aktivitet = nyStillingFraNav().withAktorId(mockBruker.getAktorId())
            .withOppfolgingsperiodeId(mockBruker.getOppfolgingsperiodeId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder);
        var resultatAktivitet = resultat.getAktiviteter().getFirst();
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertNull(resultatAktivitet.getStillingFraNavData().getCvKanDelesData());
    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataMedCVData() {
        var aktivitet = nyStillingFraNavMedCVKanDeles().withAktorId(mockBruker.getAktorId())
                .withOppfolgingsperiodeId(mockBruker.getOppfolgingsperiodeId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder);
        var resultatAktivitet = resultat.getAktiviteter().getFirst();
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertNotNull(resultatAktivitet.getStillingFraNavData().getCvKanDelesData());
        assertTrue(resultatAktivitet.getStillingFraNavData().getCvKanDelesData().getKanDeles());
    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataMedCvSvar() {
        var aktivitet = nyStillingFraNavMedCVKanDeles().withAktorId(mockBruker.getAktorId())
                .withOppfolgingsperiodeId(mockBruker.getOppfolgingsperiodeId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder);
        var resultatAktivitet = resultat.getAktiviteter().getFirst();
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertTrue(resultatAktivitet.getStillingFraNavData().getCvKanDelesData().getKanDeles());

    }

    @Test
    void hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter();
        og_veileder_har_tilgang_til_brukers_enhet();
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    void hent_aktivitet() {
        gitt_at_jeg_har_aktiviter();
        da_skal_jeg_kunne_hente_en_aktivitet();
    }

    @Test
    void hent_aktivitetsplan_med_kontorsperre() {
        gitt_at_jeg_har_aktiviteter_med_kontorsperre();
        og_veileder_har_tilgang_til_brukers_enhet();
        da_aktiviteter_med_og_uten_kontosperre_ligge_i_min_aktivitetsplan();
    }

    @Test
    void hent_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_en_aktivitet_med_kontorsperre();
        og_veileder_har_nasjonsal_tilgang_men_ikke_tilgang_til_brukers_enhet();
        da_skal_jeg_ikke_kunne_hente_noen_aktiviteter();
    }

    @Test
    void opprett_aktivitet() {
        gitt_at_jeg_har_laget_en_aktivtet();
        nar_jeg_lagrer_aktivteten();
        da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan();
    }

    @Test
    void oppdater_status() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        da_skal_min_aktivitet_fatt_ny_status();
    }

    @Test
    void oppdater_etikett() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_etiketten_pa_en_aktivitet();
        da_skal_min_aktivitet_fatt_ny_etikett();
    }

    @Test
    void hent_aktivitet_versjoner() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_flytter_en_aktivitet_til_en_annen_status();
        nar_jeg_henter_versjoner_pa_denne_aktiviten();
        da_skal_jeg_fa_versjonene_pa_denne_aktiviteten();
    }

    @Test
    void oppdater_aktivtet() {
        gitt_at_jeg_har_aktiviter();
        nar_jeg_oppdaterer_en_av_aktiviten();
        da_skal_jeg_aktiviten_vare_endret();
    }

    @Test
    void skal_ikke_kunne_endre_annet_enn_frist_pa_avtalte_aktiviter() {
        gitt_at_jeg_har_laget_en_aktivtet();
        gitt_at_jeg_har_satt_aktiviteten_til_avtalt();
        nar_jeg_lagrer_aktivteten();
        nar_jeg_oppdaterer_aktiviten();
        da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert();
    }

    private void gitt_at_jeg_har_aktiviter() {
        List<AktivitetData> aktiviter = Arrays.asList(
                nyttStillingssok(), nyttStillingssok()
        );
        gitt_at_jeg_har_folgende_aktiviteter(aktiviter);
    }

    private void gitt_at_jeg_har_aktiviteter_med_kontorsperre() {
        var enableKvp = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        navMockService.updateBruker(mockBruker, enableKvp);
        gitt_at_jeg_har_folgende_aktiviteter(List.of(
                nyttStillingssok(),
                nyttStillingssok()
        ));
        var removeKvp = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(false).build();
        navMockService.updateBruker(mockBruker, removeKvp);
        gitt_at_jeg_har_folgende_aktiviteter(List.of(
                nyttStillingssok(),
                nyttStillingssok()
        ));
    }

    private void gitt_at_jeg_har_en_aktivitet_med_kontorsperre() {
        var enableKvp = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        navMockService.updateBruker(mockBruker, enableKvp);
        gitt_at_jeg_har_folgende_aktiviteter(List.of(nyttStillingssok()));
        var removeKvp = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(false).build();
        navMockService.updateBruker(mockBruker, removeKvp);
    }

    private void gitt_at_jeg_har_folgende_aktiviteter(List<AktivitetData> aktiviteter) {
        var aktorId = mockBruker.getAktorId();
        var kontorSperreEnhet = mockBruker.getPrivatbruker().getOppfolgingsenhet();
        lagredeAktivitetsIder = aktiviteter.stream()
                .map(aktivitet -> aktivitetService.opprettAktivitet(
                    aktivitet.withAktorId(aktorId)
                            .withEndretAvType(aktorId.tilInnsenderType())
                            .withKontorsperreEnhetId(kontorSperreEnhet)
                ))
                .map(AktivitetData::getId)
                .collect(Collectors.toList());
    }

    private void gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet();
    }

    private void gitt_at_jeg_har_satt_aktiviteten_til_avtalt() {
        aktivitet.setAvtalt(true);
    }

    private void nar_jeg_lagrer_aktivteten() {
        aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockBrukersVeileder, aktivitet);
    }

    private void nar_jeg_oppdaterer_aktiviten() {

        orignalAktivitet = aktivitet.toBuilder().build();

        ValidatableResponse validatableResponse = aktivitetTestService.oppdatterAktivitet(mockBruker, mockBrukersVeileder,
                aktivitet.setBeskrivelse("noe tull")
                        .setArbeidsgiver("Justice league")
                        .setEtikett(EtikettTypeDTO.AVSLAG)
                        .setTilDato(new Date())
        );

        Response response = validatableResponse
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response();

        aktivitet = response.as(AktivitetDTO.class);
    }

    private void nar_jeg_flytter_en_aktivitet_til_en_annen_status() {
        final var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder).getAktiviteter().getFirst();
        this.aktivitet = aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockBrukersVeileder,aktivitet, nyAktivitetStatus);
    }

    private void nar_jeg_oppdaterer_etiketten_pa_en_aktivitet() {
        final var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder).getAktiviteter().getFirst();
        this.aktivitet = aktivitetTestService.oppdaterAktivitetEtikett(mockBruker, mockBrukersVeileder,aktivitet, nyAktivitetEtikett);
    }

    private List<AktivitetDTO> versjoner;

    private void nar_jeg_henter_versjoner_pa_denne_aktiviten() {
        versjoner = aktivitetTestService.hentVersjoner(aktivitet.getId(), mockBruker, mockBrukersVeileder);
    }

    private String nyLenke;
    private String nyAvsluttetKommentar;
    private Date oldOpprettetDato;

    private void nar_jeg_oppdaterer_en_av_aktiviten() {
        final var originalAktivitet = aktivitetService.hentAktivitetMedForhaandsorientering(lagredeAktivitetsIder.getFirst());
        oldOpprettetDato = originalAktivitet.getOpprettetDato();
        nyLenke = "itsOver9000.com";
        nyAvsluttetKommentar = "The more I talk, the more i understand why i'm single";

        final var nyAktivitet = originalAktivitet
                .toBuilder()
                .lenke(nyLenke)
                .avsluttetKommentar(nyAvsluttetKommentar)
                .build();

        this.aktivitet = aktivitetTestService.oppdaterAktivitetOk(mockBruker, mockBrukersVeileder, AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet, false));
        this.lagredeAktivitetsIder.set(0, Long.parseLong(this.aktivitet.getId()));
    }


    private void da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter();
        assertThat(aktiviteter, hasSize(2));
    }

    private void da_aktiviteter_med_og_uten_kontosperre_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter();
        assertThat(aktiviteter, hasSize(4));
    }

    private void da_skal_jeg_ikke_kunne_hente_noen_aktiviteter() {
        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter();
        assertThat(aktiviteter, hasSize(0));
    }

    private void da_skal_jeg_kunne_hente_en_aktivitet() {
        assertThat(lagredeAktivitetsIder.getFirst().toString(),
                equalTo((aktivitetTestService.hentAktivitet(mockBruker, mockBrukersVeileder, lagredeAktivitetsIder.getFirst().toString())).getId()));
    }

    private void da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan() {
        assertThat(aktivitetService.hentAktiviteterForAktorId(mockBruker.getAktorId()), hasSize(1));
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
        var lagretAktivitet = aktivitetTestService.hentAktivitet(mockBruker, mockBrukersVeileder, lagredeAktivitetsIder.getFirst().toString());

        assertThat(lagretAktivitet.getLenke(), equalTo(nyLenke));
        assertThat(lagretAktivitet.getAvsluttetKommentar(), equalTo(nyAvsluttetKommentar));
        assertThat(lagretAktivitet.getOpprettetDato(), equalTo(oldOpprettetDato));
    }

    private void da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert() {
        assertThat(aktivitet, equalTo(orignalAktivitet
                .setTilDato(aktivitet.getTilDato())
                .setVersjon(aktivitet.getVersjon()) //automatiske felter satt av systemet
                .setEndretAvType(aktivitet.getEndretAvType())
                .setTransaksjonsType(aktivitet.getTransaksjonsType())
                .setEndretDato(aktivitet.getEndretDato())
                .setEndretAv(mockBrukersVeileder.getNavIdent())
                .setOppfolgingsperiodeId(aktivitet.getOppfolgingsperiodeId())
        ));
    }

    private void og_veileder_har_tilgang_til_brukers_enhet() {
        aktivVeileder = mockBrukersVeileder;
    }

    private void og_veileder_har_nasjonsal_tilgang_men_ikke_tilgang_til_brukers_enhet() {
        aktivVeileder = annenMockVeilederMedNasjonalTilgang;
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
