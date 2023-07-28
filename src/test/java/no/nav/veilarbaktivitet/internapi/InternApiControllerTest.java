package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Egenaktivitet;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InternApiControllerTest extends SpringBootTestBase {
    @Test
    void hentAktiviteterTest() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO moteAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        aktivitetTestService.opprettAktivitetSomVeileder(mockVeileder, mockBruker, moteAktivitet);

        AktivitetData aktivitetData2 = AktivitetDataTestBuilder.nyEgenaktivitet().withTilDato(null);
        AktivitetDTO egenAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData2, false);

        aktivitetTestService.opprettAktivitet(mockBruker, egenAktivitet);

        // Sett bruker under KVP
        BrukerOptions kvpOptions = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(mockBruker, kvpOptions);
        aktivitetTestService.opprettAktivitetSomVeileder(mockVeileder, mockBruker, moteAktivitet);

        // Test "/internal/api/v1/aktivitet"
        List<Aktivitet> aktiviteter = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId().get())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);

        assertThat(aktiviteter).hasSize(3);
        assertThat(aktiviteter.get(1)).isInstanceOf(Egenaktivitet.class);
        assertThat(aktiviteter.get(2).getKontorsperreEnhetId()).isNotNull();

        // Lag veileder uten tilgang til mockbrukers enhet
        MockVeileder mockVeileder2 = MockNavService.createVeileder();
        mockVeileder2.setNasjonalTilgang(true);

        List<Aktivitet> aktiviteter2 = mockVeileder2.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId().get())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);

        assertThat(aktiviteter2).hasSize(2);
        assertThat(aktiviteter2.get(1)).isInstanceOf(Egenaktivitet.class);
        assertThat(aktiviteter2.stream().map(Aktivitet::getKontorsperreEnhetId).toList()).containsOnlyNulls();
    }

    @Test
    void skalFunkeForAlleAktivitettyper() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        for (AktivitetTypeDTO type : AktivitetTypeDTO.values()) {
            if (type.equals(AktivitetTypeDTO.STILLING_FRA_NAV)) {
                aktivitetTestService.opprettStillingFraNav(mockBruker);
            } else if (type == AktivitetTypeDTO.EKSTERNAKTIVITET) {
                // TODO aktivitetTestService.opprettEksternAktivitet(mockBruker);
            } else {
                AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(type);
                aktivitetTestService.opprettAktivitet(mockBruker, mockVeileder, aktivitetDTO);
            }
        }

        List<Aktivitet> aktiviteter = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId={aktorId}", mockBruker.getAktorId().get())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);

        assertThat(AktivitetTypeDTO.values()).hasSize(aktiviteter.size() + 1);
    }

    @Test
    void skalFeileNaarManglerTilgang() {
        // Forbidden (403)
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeilederUtenBruker = MockNavService.createVeileder();
        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId().get())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void skalFeilNaarManglerParameter() {
        // Bad request (400) - ingen query parameter
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void skalFeilNaarEksternBruker() {
        // Forbidden (403)
        MockBruker mockBruker = MockNavService.createHappyBruker();
        mockBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId().get())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

}
