package no.nav.veilarbaktivitet.admin

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class KasserControllerTest : SpringBootTestBase() {
    private val mockBruker by lazy { navMockService.createHappyBruker(BrukerOptions.happyBruker()) }
    private val veileder by lazy { MockNavService.createVeileder(mockBruker) }

    @Test
    fun skal_ikke_kunne_kassere_aktivitet_uten_tilgang() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        veileder
            .createRequest()
            .put("http://localhost:" + port + "/veilarbaktivitet/api/kassering/" + aktivitet.id)
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
