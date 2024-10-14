package no.nav.veilarbaktivitet.avtalt_med_nav;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetAssertUtils;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class AvtaltMedNavTest extends SpringBootTestBase {
    @Autowired
    BrukernotifikasjonAssertsConfig config;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;

    @BeforeEach
    void before() {
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(config);
    }

    public ForhaandsorienteringDTO testFho(Type type) {
        return ForhaandsorienteringDTO
                .builder()
                .type(type)
                .tekst("dette er en tekst")
                .build();
    }

    @Test
    void IkkeSendeFhoForBrukerSomIkkeKanVarsles() {
        MockBruker brukerSomIkkeKanVarsles = BrukernotifikasjonAsserts.getBrukerSomIkkeKanVarsles();
        MockVeileder veileder = MockNavService.createVeileder(brukerSomIkkeKanVarsles);
        ForhaandsorienteringDTO fho = testFho(Type.SEND_FORHAANDSORIENTERING);

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO();
        AktivitetDTO utenFHO = aktivitetTestService.opprettAktivitet(brukerSomIkkeKanVarsles, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));
        avtaltDTO.setAktivitetVersjon(Long.parseLong(utenFHO.getVersjon()));
        avtaltDTO.setForhaandsorientering(fho);
        veileder
                .createRequest(brukerSomIkkeKanVarsles)
                .and()
                .body(avtaltDTO)
                .when()
                .queryParam("aktivitetId", utenFHO.getId())
                .put("http://localhost:" + port + "/veilarbaktivitet/api/avtaltMedNav")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void IkkeOppretteFHOUtenAktivitet() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        ForhaandsorienteringDTO fho = testFho(Type.IKKE_SEND_FORHAANDSORIENTERING);

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO();
        avtaltDTO.setAktivitetVersjon(Long.MAX_VALUE);
        avtaltDTO.setForhaandsorientering(fho);

        veileder
                .createRequest(happyBruker)
                .and()
                .body(avtaltDTO)
                .when()
                .queryParam("aktivitetId", Long.MAX_VALUE)
                .put("http://localhost:" + port + "/veilarbaktivitet/api/avtaltMedNav")
                .then()
                .assertThat()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void IkkeOppretteFHOFEilAktivitetVersion() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        AktivitetDTO utenFHO = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));
        ForhaandsorienteringDTO fho = testFho(Type.IKKE_SEND_FORHAANDSORIENTERING);

        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO();
        avtaltDTO.setAktivitetVersjon(Long.parseLong(utenFHO.getVersjon()) + 1);
        avtaltDTO.setForhaandsorientering(fho);

        veileder
                .createRequest(happyBruker)
                .and()
                .body(avtaltDTO)
                .when()
                .queryParam("aktivitetId", utenFHO.getId())
                .put("http://localhost:" + port + "/veilarbaktivitet/api/avtaltMedNav")
                .then()
                .assertThat()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void setteAvtaltUtenFHOForBrukerSomIkkeKanVarsles() {
        MockBruker brukerSomIkkeKanVarsles = BrukernotifikasjonAsserts.getBrukerSomIkkeKanVarsles();
        MockVeileder veileder = MockNavService.createVeileder(brukerSomIkkeKanVarsles);
        ForhaandsorienteringDTO fho = testFho(Type.IKKE_SEND_FORHAANDSORIENTERING);

        oppretFHO(fho, veileder, brukerSomIkkeKanVarsles);

        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    @Test
    void sendeForhondsorentering() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        ForhaandsorienteringDTO fho = testFho(Type.SEND_FORHAANDSORIENTERING);

        oppretFHO(fho, veileder, happyBruker);

        brukernotifikasjonAsserts.assertOppgaveSendt(happyBruker.getFnrAsFnr());
    }

    @Test
    void sendeForhondsorenteringFor11_9() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        ForhaandsorienteringDTO fho = testFho(Type.SEND_PARAGRAF_11_9);

        oppretFHO(fho, veileder, happyBruker);

        brukernotifikasjonAsserts.assertOppgaveSendt(happyBruker.getFnrAsFnr());
    }

    @Test
    void skalIkkeSendeVarselForIkkeSend() {
        MockBruker happyBruker = MockNavService.createHappyBruker();
        MockVeileder veileder = MockNavService.createVeileder(happyBruker);
        ForhaandsorienteringDTO fho = testFho(Type.IKKE_SEND_FORHAANDSORIENTERING);

        oppretFHO(fho, veileder, happyBruker);

        brukernotifikasjonAsserts.assertSkalIkkeHaProdusertFlereMeldinger();
    }

    private AktivitetDTO oppretFHO(ForhaandsorienteringDTO fho, MockVeileder veileder, MockBruker happyBruker) {
        AvtaltMedNavDTO avtaltDTO = new AvtaltMedNavDTO();
        AktivitetDTO utenFHO = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN));
        avtaltDTO.setAktivitetVersjon(Long.parseLong(utenFHO.getVersjon()));
        avtaltDTO.setForhaandsorientering(fho);
        AktivitetDTO medFHO = veileder
                .createRequest(happyBruker)
                .and()
                .body(avtaltDTO)
                .when()
                .queryParam("aktivitetId", utenFHO.getId())
                .put("http://localhost:" + port + "/veilarbaktivitet/api/avtaltMedNav")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(AktivitetDTO.class);

        fho.setId(medFHO.getForhaandsorientering().getId());
        utenFHO
                .setForhaandsorientering(fho)
                .setAvtalt(true);
        AktivitetAssertUtils.assertOppdatertAktivitet(utenFHO, medFHO);
        return medFHO;
    }
}
