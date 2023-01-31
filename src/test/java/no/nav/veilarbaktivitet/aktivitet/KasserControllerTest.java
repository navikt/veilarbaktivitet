package no.nav.veilarbaktivitet.aktivitet;

import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class KasserControllerTest extends SpringBootTestBase {
    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder veileder = MockNavService.createVeileder(mockBruker);

    @Test
    public void skal_ikke_kunne_kassere_aktivitet_uten_tilgang() {
        var aktivitet = aktivitetTestService.opprettAktivitet(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING));
        kasserTestService.kasserAktivitet(veileder, Long.parseLong(aktivitet.getId()))
                .assertThat()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }
}