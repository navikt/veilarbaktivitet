package no.nav.veilarbaktivitet.arkivering

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.veilarbaktivitet.SpringBootTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class ArkiveringsControllerTest: SpringBootTestBase() {


    @Test
    fun `Når man arkiverer skal man samle inn data og sende til orkivar`() {
        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering"
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(bruker)
        veileder
            .createRequest(bruker)
            .post(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())

        // TODO: Definer navn i oppsett for å gjøre asserten mer forståelig
        verify(
            exactly(1 ), postRequestedFor(urlEqualTo("/orkivar/arkiver"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
            .withRequestBody(
                equalToJson("""
                {
                  "metadata": {
                    "navn": "TRIVIELL SKILPADDE",
                    "fnr": "01015450300"
                  }
                }
            """.trimIndent())
            ));
    }
}
