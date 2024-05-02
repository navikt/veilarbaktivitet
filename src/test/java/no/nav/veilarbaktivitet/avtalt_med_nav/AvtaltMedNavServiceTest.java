package no.nav.veilarbaktivitet.avtalt_med_nav;

import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.feil.EndringAvUtdatertVersjonException;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.equalTo;

class AvtaltMedNavServiceTest extends SpringBootTestBase {

    private static final MockBruker bruker = MockNavService.createHappyBruker();
    private static final MockVeileder veileder = MockNavService.createVeileder(bruker);

    private static final Person.AktorId AKTOR_ID = bruker.getAktorIdAsAktorId();
    private static final NavIdent veilederIdent = veileder.getNavIdentAsNavident();

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AktivitetDAO aktivitetDAO;

    @Autowired
    AvtaltMedNavService avtaltMedNavService;
    final String defaultTekst = "tekst";
    final Type defaultType = Type.SEND_FORHAANDSORIENTERING;

    @Test
    void opprettFHO_oppdatererInterneFHOVerdier() {

        AktivitetDTO aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID));
        var fho = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDTO.getId()));

        Assertions.assertEquals(aktivitetDTO.getId(), fho.getAktivitetId());
        // FHO aktivitetsversjon vil faktisk peke på forrige aktivitetsversjon
        Assertions.assertEquals(Long.parseLong(aktivitetDTO.getVersjon()) - 1L, Long.parseLong(fho.getAktivitetVersjon()));
        Assertions.assertEquals(AKTOR_ID.get(), fho.getAktorId().get());
        Assertions.assertEquals(veilederIdent.toString(), fho.getOpprettetAv());
        Assertions.assertNull(fho.getLestDato());

    }

    @Test
    void opprettFHO_oppdatererAktivitetDTO() {
        var aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID));
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();
        var nyAktivitetMedFHO = aktivitetDAO.hentAktivitet(Long.parseLong(aktivitetDTO.getId()));

        Assertions.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assertions.assertEquals(defaultType, aktivitetDTOFHO.getType());
        Assertions.assertNotNull(aktivitetDTOFHO.getId());
        Assertions.assertNull(aktivitetDTOFHO.getLestDato());
        Assertions.assertEquals(AktivitetTransaksjonsType.AVTALT, nyAktivitetMedFHO.getTransaksjonsType());

    }

    @Test
    void opprettFHO_medTomTekst_setterTekstenTilNull() {
        var aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID);

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst("").build();

        var aktivitetDTO = opprettAktivitetMedFHO(aktivitetData, fhoDTO);
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();

        Assertions.assertNull(aktivitetDTOFHO.getTekst());

    }

    @Test
    void opprettFHO_maaHaMatchendeAktivitet() {
        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst(defaultTekst).lestDato(null).build();

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(fhoDTO)
                .setAktivitetVersjon(5L);
        Assertions.assertThrows(DataIntegrityViolationException.class, () ->
                avtaltMedNavService.opprettFHO(avtaltDTO, 999999, AKTOR_ID, veilederIdent));

    }

    @Test
    void hentFhoForAktivitet_henterRiktigFho() {
        var aktivitetData = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID));

        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetData.getId()));

        Assertions.assertEquals(aktivitetData.getId(), aktivitetDTOFHO.getAktivitetId());
        Assertions.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assertions.assertEquals(defaultType, aktivitetDTOFHO.getType());
    }

    @Test
    void markerSomLest_oppdatererAktivitetDTO() {
        var aktivitetDto = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID));

        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDto.getId()));

        avtaltMedNavService.markerSomLest(aktivitetDTOFHO, AKTOR_ID, Long.valueOf(aktivitetDto.getVersjon()));

        var nyAktivitet = aktivitetDAO.hentAktivitet(Long.parseLong(aktivitetDto.getId()));

        Assertions.assertEquals(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST, nyAktivitet.getTransaksjonsType());

    }

    @Test
    void markerSomLest_setterVarselFerdig() {
        var aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID));
        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDTO.getId()));

        avtaltMedNavService.markerSomLest(aktivitetDTOFHO, AKTOR_ID, Long.valueOf(aktivitetDTO.getVersjon()));
        var nyFHO = avtaltMedNavService.hentFHO(aktivitetDTO.getForhaandsorientering().getId());

        Assertions.assertNotNull(nyFHO.getLestDato());
        Assertions.assertEquals(nyFHO.getLestDato(), nyFHO.getVarselSkalStoppesDato());

    }

    @Test
    void settVarselFerdig_stopperAktivtVarsel() {
        var aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID);
        var opprettetAktivitetMedFHO = opprettAktivitetMedDefaultFHO(aktivitetData);
        boolean varselStoppet = avtaltMedNavService.settVarselFerdig(opprettetAktivitetMedFHO.getForhaandsorientering().getId());
        var nyFHO = avtaltMedNavService.hentFHO(opprettetAktivitetMedFHO.getForhaandsorientering().getId());

        Assertions.assertTrue(varselStoppet);
        Assertions.assertNotNull(nyFHO.getVarselSkalStoppesDato());

    }

    @Test
    void settVarselFerdig_ForhåndsorienteringsIdErNULL_returnererFalse() {
        var varselStoppet = avtaltMedNavService.settVarselFerdig(null);

        Assertions.assertFalse(varselStoppet);
    }

    @Test
    void oppdateringer_paa_samme_versjon_skal_feile() {
        var aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.BEHANDLING);
        var opprettetAktivitet = aktivitetTestService.opprettAktivitet(bruker, aktivitet);
        var aktivitetId = Long.parseLong(opprettetAktivitet.getId());
        var avtaltDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(Long.parseLong(opprettetAktivitet.getVersjon()))
                .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(defaultType)
                        .tekst(defaultTekst).lestDato(null).build());
        aktivitetTestService.opprettFHOForInternAktivitet(bruker, veileder, avtaltDTO, aktivitetId);
        aktivitetTestService.opprettFHOForInternAktivitetRequest(bruker, veileder, avtaltDTO, aktivitetId)
                .body("message", equalTo("Feil aktivitetversjon"))
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void oppdateringer_paa_samme_versjon_skal_feile_innenfor_controller_for_aa_hindre_race_conditions() {
        var aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.BEHANDLING);
        var opprettetAktivitet = aktivitetTestService.opprettAktivitet(bruker, aktivitet);
        var aktivitetId = Long.parseLong(opprettetAktivitet.getId());


        var avtaltDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(Long.parseLong(opprettetAktivitet.getVersjon()))
                .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(defaultType)
                        .tekst(defaultTekst).lestDato(null).build());
        Person.AktorId aktorIdAsAktorId = bruker.getAktorIdAsAktorId();
        NavIdent navIdentAsNavident = veileder.getNavIdentAsNavident();
        transactionTemplate.executeWithoutResult((transactionStatus) -> avtaltMedNavService.opprettFHO(avtaltDTO, aktivitetId, aktorIdAsAktorId, navIdentAsNavident));
        Assertions.assertThrows(EndringAvUtdatertVersjonException.class,
                () -> avtaltMedNavService.opprettFHO(avtaltDTO, aktivitetId, aktorIdAsAktorId, navIdentAsNavident),
                "no.nav.veilarbaktivitet.aktivitet.feil.EndringAvUtdatertVersjonException: Forsøker å oppdatere en utdatert aktivitetsversjon."
        );

    }


    @Test
    void skal_ikke_kunne_opprette_FHO_pa_aktivitet_med_FHO() {
        var aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.BEHANDLING);
        var opprettetAktivitet = aktivitetTestService.opprettAktivitet(bruker, aktivitet);
        var aktivitetId = Long.parseLong(opprettetAktivitet.getId());
        var avtaltDTO = new AvtaltMedNavDTO()
                .setAktivitetVersjon(Long.parseLong(opprettetAktivitet.getVersjon()))
                .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(defaultType)
                        .tekst(defaultTekst).lestDato(null).build());
        var oppdatertAktivitet = aktivitetTestService.opprettFHOForInternAktivitet(bruker, veileder, avtaltDTO, aktivitetId);
        var oppdaterAvtaltDto = avtaltDTO.setAktivitetVersjon(Long.parseLong(oppdatertAktivitet.getVersjon()));
        aktivitetTestService.opprettFHOForInternAktivitetRequest(bruker, veileder, oppdaterAvtaltDto, aktivitetId)
                .body("message", equalTo("Aktiviteten har allerede en forhåndsorientering"))
                .statusCode(HttpStatus.CONFLICT.value());
    }

    private AktivitetDTO opprettAktivitetMedDefaultFHO(AktivitetData aktivitetData) {

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst(defaultTekst).lestDato(null).build();

        return opprettAktivitetMedFHO(aktivitetData, fhoDTO);
    }

    private AktivitetDTO opprettAktivitetMedFHO(AktivitetData aktivitetData, ForhaandsorienteringDTO forhaandsorienteringDTO) {
        AktivitetData nyAktivitet = aktivitetDAO.opprettNyAktivitet(aktivitetData);

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(forhaandsorienteringDTO)
                .setAktivitetVersjon(nyAktivitet.getVersjon());

        return avtaltMedNavService.opprettFHO(avtaltDTO, nyAktivitet.getId(), AKTOR_ID, veilederIdent);
    }

}
