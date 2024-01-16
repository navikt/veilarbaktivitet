package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.SpringBootTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ArkiveringsControllerTest: SpringBootTestBase() {

    private val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering"

    @Test
    fun `Når man arkvierer skal man samle inn data og sende til orkivar`() {
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(bruker)

        veileder
            .createRequest(bruker)
            .post(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())



    }
}
