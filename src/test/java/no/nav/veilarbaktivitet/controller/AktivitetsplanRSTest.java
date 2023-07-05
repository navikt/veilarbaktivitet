package no.nav.veilarbaktivitet.controller;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.val;
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
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.mock.TestData.KJENT_KONTORSPERRE_ENHET_ID;
import static no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Aktivitetsplan interaksjoner der pålogget bruker er saksbehandler
 */
// TODO: 19/01/2023 skriv om til nye test rammeverk (SpringBootTestBase)

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
    private MockVeileder mockVeileder;



    @BeforeEach
    void moreSettup() {
        mockBruker = MockNavService.createHappyBruker();
        mockVeileder = MockNavService.createVeileder(mockBruker);
    }

    @AfterEach
    void cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }


    @Test
    void hentAktivitetVersjoner_returnererIkkeForhaandsorientering() {
        var aktivitet = aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()));
        Long aktivitetId = aktivitet.getId();
        aktivitetService.oppdaterStatus(aktivitet, aktivitet.withStatus(AktivitetStatus.GJENNOMFORES), Person.aktorId(mockBruker.getAktorId()).tilIdent());
        var sisteAktivitetVersjon = aktivitetService.hentAktivitetMedForhaandsorientering(aktivitetId);
        var fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build();
        avtaltMedNavService.opprettFHO(new AvtaltMedNavDTO().setAktivitetVersjon(sisteAktivitetVersjon.getVersjon()).setForhaandsorientering(fho), aktivitetId, Person.aktorId(mockBruker.getAktorId()), NavIdent.of("V123"));
        var resultat = aktivitetTestService.hentVersjoner(aktivitetId + "", mockBruker, mockVeileder);


        assertEquals(3, resultat.size());
        assertEquals(AktivitetTransaksjonsType.AVTALT, resultat.get(0).getTransaksjonsType());
        assertNull(resultat.get(0).getForhaandsorientering());
        assertNull(resultat.get(1).getForhaandsorientering());
        assertNull(resultat.get(2).getForhaandsorientering());
    }

    @Test
    void hentAktivitetsplan_henterAktiviteterMedForhaandsorientering() {
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()));
        aktivitetDAO.opprettNyAktivitet(nyttStillingssok().withAktorId(mockBruker.getAktorId()));

        var fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build();
        avtaltMedNavService.opprettFHO(new AvtaltMedNavDTO().setAktivitetVersjon(aktivitetData.getVersjon()).setForhaandsorientering(fho), aktivitetData.getId(), Person.aktorId(mockBruker.getAktorId()), NavIdent.of("V123"));
        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder);
        assertNull(resultat.getAktiviteter().get(0).getForhaandsorientering());
        assertNotNull(resultat.getAktiviteter().get(1).getForhaandsorientering());


    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataUtenCVData() {
        var aktivitet = nyStillingFraNav().withAktorId(mockBruker.getAktorId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder);
        var resultatAktivitet = resultat.getAktiviteter().get(0);
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertNull(resultatAktivitet.getStillingFraNavData().getCvKanDelesData());

    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataMedCVData() {
        var aktivitet = nyStillingFraNavMedCVKanDeles().withAktorId(mockBruker.getAktorId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder);
        var resultatAktivitet = resultat.getAktiviteter().get(0);
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertNotNull(resultatAktivitet.getStillingFraNavData().getCvKanDelesData());
        assertTrue(resultatAktivitet.getStillingFraNavData().getCvKanDelesData().getKanDeles());
    }

    @Test
    void hentAktivitetsplan_henterStillingFraNavDataMedCvSvar() {
        var aktivitet = nyStillingFraNavMedCVKanDeles().withAktorId(mockBruker.getAktorId());
        AktivitetData aktivitetData = aktivitetDAO.opprettNyAktivitet(aktivitet);

        var resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder);
        var resultatAktivitet = resultat.getAktiviteter().get(0);
        assertEquals(1, resultat.getAktiviteter().size());
        assertEquals(String.valueOf(aktivitetData.getId()), resultatAktivitet.getId());
        assertTrue(resultatAktivitet.getStillingFraNavData().getCvKanDelesData().getKanDeles());

    }

    @Test
    void hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter();
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
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan();
    }

    @Test
    void hent_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_en_aktivitet_med_kontorsperre();
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
        gitt_at_jeg_har_folgende_aktiviteter(Arrays.asList(
                nyttStillingssok(),
                nyttStillingssok().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID),
                nyttStillingssok(),
                nyttStillingssok().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID)
        ));
    }

    private void gitt_at_jeg_har_en_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_folgende_aktiviteter(Collections.singletonList(
                nyttStillingssok().withKontorsperreEnhetId(KJENT_KONTORSPERRE_ENHET_ID)
        ));
    }

    private void gitt_at_jeg_har_folgende_aktiviteter(List<AktivitetData> aktiviteter) {
        lagredeAktivitetsIder = aktiviteter.stream()
                .map(aktivitet -> aktivitetService.opprettAktivitet(Person.aktorId(mockBruker.getAktorId()), aktivitet, Person.aktorId(mockBruker.getAktorId()).tilIdent()).getId())
                .collect(Collectors.toList());
    }

    private void gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet();
    }

    private void gitt_at_jeg_har_satt_aktiviteten_til_avtalt() {
        aktivitet.setAvtalt(true);
    }

    private void nar_jeg_lagrer_aktivteten() {
        aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, mockVeileder, aktivitet);
    }

    private void nar_jeg_oppdaterer_aktiviten() {

        orignalAktivitet = aktivitet.toBuilder().build();

        ValidatableResponse validatableResponse = aktivitetTestService.oppdatterAktivitet(mockBruker, mockVeileder,
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
        val aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder).aktiviteter.get(0);
        this.aktivitet = aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockVeileder,aktivitet, nyAktivitetStatus);
    }

    private void nar_jeg_oppdaterer_etiketten_pa_en_aktivitet() {
        val aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder).aktiviteter.get(0);
        this.aktivitet = aktivitetTestService.oppdaterAktivitetEtikett(mockBruker, mockVeileder,aktivitet, nyAktivitetEtikett);
    }

    private List<AktivitetDTO> versjoner;

    private void nar_jeg_henter_versjoner_pa_denne_aktiviten() {
        versjoner = aktivitetTestService.hentVersjoner(aktivitet.getId(), mockBruker, mockVeileder);
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

        this.aktivitet = aktivitetTestService.oppdaterAktivitetOk(mockBruker, mockVeileder, AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet, false));
        this.lagredeAktivitetsIder.set(0, Long.parseLong(this.aktivitet.getId()));
    }


    private void da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder).aktiviteter;
        assertThat(aktiviteter, hasSize(2));
    }

    private void da_skal_jeg_ikke_kunne_hente_noen_aktiviteter() {
        List<AktivitetDTO> aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockVeileder).aktiviteter;
        assertThat(aktiviteter, hasSize(0));
    }

    private void da_skal_jeg_kunne_hente_en_aktivitet() {
        assertThat(lagredeAktivitetsIder.get(0).toString(),
                equalTo((aktivitetTestService.hentAktivitet(mockBruker, mockVeileder, lagredeAktivitetsIder.get(0).toString())).getId()));
    }

    private void da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan() {
        assertThat(aktivitetService.hentAktiviteterForAktorId(Person.aktorId(mockBruker.getAktorId())), hasSize(1));
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
        var lagretAktivitet = aktivitetTestService.hentAktivitet(mockBruker, mockVeileder, lagredeAktivitetsIder.get(0).toString());

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
                .setEndretAv(mockVeileder.getNavIdent())
                .setOppfolgingsperiodeId(aktivitet.getOppfolgingsperiodeId())
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
