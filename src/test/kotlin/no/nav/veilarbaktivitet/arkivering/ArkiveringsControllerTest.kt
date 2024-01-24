package no.nav.veilarbaktivitet.arkivering

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.arkivering.mapper.norskDato
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivTypeTekst
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
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
        val sisteOppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }
        val jobbAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiode).build()

        aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitet)

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
                        "navn": "${navn.tilFornavnMellomnavnEtternavn()}",
                        "fnr": "${bruker.fnr}"
                      },
                      "aktiviteter": [
                        {
                          "tittel": "tittel",
                          "type": "Jobb jeg har nå",
                          "status": "Planlagt",
                          "detaljer": [
                            {
                              "stil": "HALV_LINJE",
                              "tittel": "Fra dato",
                              "tekst": "${jobbAktivitet.fraDato.norskDato()}"
                            },
                            {
                              "stil": "HALV_LINJE",
                              "tittel": "Til dato",
                              "tekst": "${jobbAktivitet.tilDato.norskDato()}"
                            },
                            {
                              "stil": "HALV_LINJE",
                              "tittel": "Stillingsandel",
                              "tekst": "HELTID"
                            },
                            {
                              "stil": "HALV_LINJE",
                              "tittel": "Arbeidsgiver",
                              "tekst": "Vikar"
                            },
                            {
                              "stil": "HALV_LINJE",
                              "tittel": "Ansettelsesforhold",
                              "tekst": "7,5 timer"
                            },
                            {
                              "stil": "PARAGRAF",
                              "tittel": "Beskrivelse",
                              "tekst": "beskrivelse"
                            }
                          ]
                        }
                      ]
                    }
            """.trimIndent())
            ))
    }
}
