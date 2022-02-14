package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Egenaktivitet;
import no.nav.veilarbaktivitet.internapi.model.Mote;
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InternApiControllerTest extends SpringBootTestBase {

    @Test
    public void hentAktiviteterTest() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyMoteAktivitet();
        AktivitetDTO moteAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);

        AktivitetDTO opprettetAktivitet = aktivitetTestService.opprettAktivitetSomVeileder(port, mockVeileder, mockBruker, moteAktivitet);

        // Test "/internal/api/v1/aktivitet/{aktivitetId}"
        Mote aktivitet = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet/{aktivitetId}", opprettetAktivitet.getId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .as(Mote.class);

        Assertions.assertThat(aktivitet).isInstanceOf(Mote.class);

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(aktivitet.getAktivitetType().name()).isEqualTo(opprettetAktivitet.getType().name());
            a.assertThat(Date.from(aktivitet.getOpprettetDato().toInstant())).isEqualTo(opprettetAktivitet.getOpprettetDato());
            a.assertThat(aktivitet.getReferat()).isEqualTo(opprettetAktivitet.getReferat());
            a.assertAll();
        });

        AktivitetData aktivitetData2 = AktivitetDataTestBuilder.nyEgenaktivitet().withTilDato(null);
        AktivitetDTO egenAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData2, false);

        aktivitetTestService.opprettAktivitet(port, mockBruker, egenAktivitet);

        // Sett bruker under KVP
        BrukerOptions kvpOptions = mockBruker.getBrukerOptions().toBuilder().erUnderKvp(true).build();
        MockNavService.updateBruker(mockBruker, kvpOptions);
        aktivitetTestService.opprettAktivitetSomVeileder(port, mockVeileder, mockBruker, moteAktivitet);

        // Test "/internal/api/v1/aktivitet"
        List<Aktivitet> aktiviteter = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);

        assertThat(aktiviteter).hasSize(2);
        assertThat(aktiviteter.get(1)).isInstanceOf(Egenaktivitet.class);

        List<Aktivitet> aktiviteter2 = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?oppfolgingsperiodeId=" + mockBruker.getOppfolgingsperiode())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);
        assertThat(aktiviteter2).hasSameElementsAs(aktiviteter);

        List<Aktivitet> aktiviteter3 = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId={aktorId}&oppfolgingsperiodeId={oppfolgingsperiodeId}", mockBruker.getAktorId(), mockBruker.getOppfolgingsperiode())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);
        assertThat(aktiviteter3).hasSameElementsAs(aktiviteter);
    }

    @Test
    public void skalFunkeForAlleAktivitettyper() {
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);

        for (AktivitetTypeDTO type : AktivitetTypeDTO.values()) {
            if (type.equals(AktivitetTypeDTO.STILLING_FRA_NAV)) {
                aktivitetTestService.opprettStillingFraNav(mockBruker, port);
            } else {
                AktivitetDTO aktivitetDTO = AktivitetDtoTestBuilder.nyAktivitet(type);
                aktivitetTestService.opprettAktivitet(port, mockBruker, mockVeileder, aktivitetDTO);
            }
        }

        List<Aktivitet> aktiviteter = mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId={aktorId}", mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.OK.value())
                .extract()
                .response()
                .jsonPath().getList(".", Aktivitet.class);

        assertThat(AktivitetTypeDTO.values().length).isEqualTo(aktiviteter.size());
    }

    @Test
    public void skalFeileNaarManglerTilgang() {
        // Forbidden (403)
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeilederUtenBruker = MockNavService.createVeileder();
        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());

        AktivitetData aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet();
        AktivitetDTO egenAktivitet = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false);
        aktivitetTestService.opprettAktivitet(port, mockBruker, egenAktivitet);

        mockVeilederUtenBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?oppfolgingsperiodeId=" + mockBruker.getOppfolgingsperiode().toString())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void skalFeilNaarManglerParameter() {
        // Bad request (400) - ingen query parameter
        MockBruker mockBruker = MockNavService.createHappyBruker();
        MockVeileder mockVeileder = MockNavService.createVeileder(mockBruker);
        mockVeileder.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet")
                .then()
                .assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void skalFeilNaarEksternBruker() {
        // Forbidden (403)
        MockBruker mockBruker = MockNavService.createHappyBruker();
        mockBruker.createRequest()
                .get("http://localhost:" + port + "/veilarbaktivitet/internal/api/v1/aktivitet?aktorId=" + mockBruker.getAktorId())
                .then()
                .assertThat().statusCode(HttpStatus.FORBIDDEN.value());
    }

}