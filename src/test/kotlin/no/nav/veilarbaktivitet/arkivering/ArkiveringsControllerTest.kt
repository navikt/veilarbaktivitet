package no.nav.veilarbaktivitet.arkivering

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.person.Navn
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class ArkiveringsControllerTest: SpringBootTestBase() {


    @Test
    fun `Når man arkiverer skal man samle inn data og sende til orkivar`() {
        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering"
        val navn = Navn("Sølvi", null, "Normalbakke")
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().navn(navn).build()
        val bruker = navMockService.createHappyBruker(brukerOptions)
        val veileder = navMockService.createVeileder(bruker)
        veileder
            .createRequest(bruker)
            .post(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())

        verify(
            exactly(1 ), postRequestedFor(urlEqualTo("/orkivar/arkiver"))
            .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
            .withRequestBody(
                equalToJson("""
                {
                  "metadata": {
                    "navn": "${navn.fornavn} ${navn.etternavn}",
                    "fnr": "${bruker.fnr}"
                  }
                }
            """.trimIndent())
            ))
    }
}
