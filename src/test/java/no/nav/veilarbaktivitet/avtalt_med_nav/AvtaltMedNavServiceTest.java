package no.nav.veilarbaktivitet.avtalt_med_nav;

import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class AvtaltMedNavServiceTest extends SpringBootTestBase {

    private static final MockBruker bruker= MockNavService.createHappyBruker();
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
    public void opprettFHO_oppdatererInterneFHOVerdier() {

        AktivitetDTO aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get()));
        var fho = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDTO.getId()));

        Assert.assertEquals(aktivitetDTO.getId(), fho.getAktivitetId());
        // FHO aktivitetsversjon vil faktisk peke på forrige aktivitetsversjon
        Assert.assertEquals(Long.parseLong(aktivitetDTO.getVersjon()) - 1L, Long.parseLong(fho.getAktivitetVersjon()));
        Assert.assertEquals(AKTOR_ID.get(), fho.getAktorId().get());
        Assert.assertEquals(veilederIdent.toString(), fho.getOpprettetAv());
        assertNull(fho.getLestDato());

    }

    @Test
    public void opprettFHO_oppdatererAktivitetDTO() {
        var aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get()));
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();
        var nyAktivitetMedFHO = aktivitetDAO.hentAktivitet(Long.parseLong(aktivitetDTO.getId()));

        Assert.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assert.assertEquals(defaultType, aktivitetDTOFHO.getType());
        Assert.assertNotNull(aktivitetDTOFHO.getId());
        Assert.assertNull(aktivitetDTOFHO.getLestDato());
        Assert.assertEquals(AktivitetTransaksjonsType.AVTALT, nyAktivitetMedFHO.getTransaksjonsType());

    }

    @Test
    public void opprettFHO_medTomTekst_setterTekstenTilNull() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst("").build();

        var aktivitetDTO = opprettAktivitetMedFHO(aktivitetData, fhoDTO);
        var aktivitetDTOFHO = aktivitetDTO.getForhaandsorientering();

        Assert.assertNull( aktivitetDTOFHO.getTekst());

    }

    @Test(expected = DataIntegrityViolationException.class)
    public void opprettFHO_maaHaMatchendeAktivitet() {
        var fhoDTO = ForhaandsorienteringDTO.builder()
                .type(defaultType)
                .tekst(defaultTekst).lestDato(null).build();

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO()
                .setForhaandsorientering(fhoDTO)
                .setAktivitetVersjon(5L);
        avtaltMedNavService.opprettFHO(avtaltDTO, 999999, AKTOR_ID, veilederIdent);

    }

    @Test
    public void hentFhoForAktivitet_henterRiktigFho() {
        var aktivitetData = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get()));

        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetData.getId()));

        Assert.assertEquals(aktivitetData.getId(), aktivitetDTOFHO.getAktivitetId());
        Assert.assertEquals(defaultTekst, aktivitetDTOFHO.getTekst());
        Assert.assertEquals(defaultType, aktivitetDTOFHO.getType());
    }

    @Test
    public void markerSomLest_oppdatererAktivitetDTO() {
        var aktivitetDto = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get()));

        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDto.getId()));

        avtaltMedNavService.markerSomLest(aktivitetDTOFHO, AKTOR_ID);

        var nyAktivitet = aktivitetDAO.hentAktivitet(Long.parseLong(aktivitetDto.getId()));

        Assert.assertEquals(AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST, nyAktivitet.getTransaksjonsType());

    }

    @Test
    public void markerSomLest_setterVarselFerdig() {
        var aktivitetDTO = opprettAktivitetMedDefaultFHO(AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get()));
        var aktivitetDTOFHO = avtaltMedNavService.hentFhoForAktivitet(Long.parseLong(aktivitetDTO.getId()));

        avtaltMedNavService.markerSomLest(aktivitetDTOFHO, AKTOR_ID);
        var nyFHO = avtaltMedNavService.hentFHO(aktivitetDTO.getForhaandsorientering().getId());

        Assert.assertNotNull(nyFHO.getLestDato());
        Assert.assertEquals(nyFHO.getLestDato(), nyFHO.getVarselSkalStoppesDato());

    }

    @Test
    public void settVarselFerdig_stopperAktivtVarsel() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());
        var opprettetAktivitetMedFHO = opprettAktivitetMedDefaultFHO(aktivitetData);
        boolean varselStoppet = avtaltMedNavService.settVarselFerdig(opprettetAktivitetMedFHO.getForhaandsorientering().getId());
        var nyFHO = avtaltMedNavService.hentFHO(opprettetAktivitetMedFHO.getForhaandsorientering().getId());

        Assert.assertTrue(varselStoppet);
        Assert.assertNotNull(nyFHO.getVarselSkalStoppesDato());

    }

    @Test
    public void service_should_reject_old_versions_of_aktivitet() {
        var aktivitetData =  AktivitetDataTestBuilder.nyEgenaktivitet().withAktorId(AKTOR_ID.get());

        var opprettetAktivitetMedFHO = opprettAktivitetMedDefaultFHO(aktivitetData);
        // Oppretter forhåndorientering på nytt dirkte i servicen
        assertThrows(
            DataIntegrityViolationException.class,
                () -> transactionTemplate.executeWithoutResult((transactionStatus) -> avtaltMedNavService.opprettFHO(
                new AvtaltMedNavDTO()
                    .setAktivitetVersjon(aktivitetData.getVersjon())
                    .setForhaandsorientering(ForhaandsorienteringDTO.builder()
                        .type(defaultType)
                        .tekst(defaultTekst)
                        .lestDato(null)
                        .build())
                ,
                aktivitetData.getId(),
                AKTOR_ID,
                veilederIdent
            ))
        );
        assertNotNull(avtaltMedNavService.hentFhoForAktivitet(aktivitetData.getId()));

    }

    @Test
    public void settVarselFerdig_ForhåndsorienteringsIdErNULL_returnererFalse() {
        var varselStoppet = avtaltMedNavService.settVarselFerdig(null);

        Assert.assertFalse(varselStoppet);
    }

    @Test
    public void oppdateringer_paa_samme_versjon_skal_feile(){
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
    public void oppdateringer_paa_samme_versjon_skal_feile_innenfor_controller_for_aa_hindre_race_conditions(){
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
        assertThrows(
                "java.lang.IllegalStateException: Forsøker å oppdatere en utdatert aktivitetsversjon.",
                IllegalStateException.class, () -> avtaltMedNavService.opprettFHO(avtaltDTO, aktivitetId, aktorIdAsAktorId, navIdentAsNavident)
                );

    }


    @Test
    public void skal_ikke_kunne_opprette_FHO_pa_aktivitet_med_FHO() {
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
