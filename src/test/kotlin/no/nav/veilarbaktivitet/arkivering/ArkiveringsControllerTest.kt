package no.nav.veilarbaktivitet.arkivering

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.arkivering.mapper.norskDato
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

internal class ArkiveringsControllerTest: SpringBootTestBase() {


    @Test
    fun `Når man arkiverer skal man samle inn data og sende til orkivar`() {
        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering"
        val navn = Navn("Sølvi", null, "Normalbakke")
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().navn(navn).build()
        val bruker = navMockService.createHappyBruker(brukerOptions)
        val veileder = navMockService.createVeileder(bruker)
        val sisteOppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }

        val jobbAktivitetPlanlegger = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiode).build()
        jobbAktivitetPlanlegger.status = AktivitetStatus.PLANLAGT
        val opprettetJobbAktivitet = aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitetPlanlegger)

        val jobbAktivitetAvbrutt = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiode).build()
        jobbAktivitetAvbrutt.status = AktivitetStatus.AVBRUTT
        aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitetAvbrutt)

        stubDialogTråder(fnr = bruker.fnr, oppfølgingsperiode = sisteOppfølgingsperiode.oppfolgingsperiode.toString(), aktivitetId = opprettetJobbAktivitet.id)

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
                      "aktiviteter" : {
                        "Planlagt" : [ {
                          "tittel" : "tittel",
                          "type" : "Jobb jeg har nå",
                          "status" : "Planlagt",
                          "detaljer" : [ {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Fra dato",
                            "tekst": "${jobbAktivitetPlanlegger.fraDato.norskDato()}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Til dato",
                            "tekst": "${jobbAktivitetPlanlegger.tilDato.norskDato()}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Stillingsandel",
                            "tekst" : "HELTID"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Arbeidsgiver",
                            "tekst" : "Vikar"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Ansettelsesforhold",
                            "tekst" : "7,5 timer"
                          }, {
                            "stil" : "PARAGRAF",
                            "tittel" : "Beskrivelse",
                            "tekst" : "beskrivelse"
                          } ],
                          "meldinger" : [ {
                            "avsender" : "VEILEDER",
                            "sendt" : "05 februar 2024 kl. 14:31",
                            "lest" : true,
                            "viktig" : false,
                            "tekst" : "wehfuiehwf<br/><br/>Hilsen F_994188 E_994188"
                          }, {
                            "avsender" : "BRUKER",
                            "sendt" : "05 februar 2024 kl. 14:31",
                            "lest" : true,
                            "viktig" : false,
                            "tekst" : "Jada"
                          } ]
                        } ],
                        "Avbrutt" : [ {
                          "tittel" : "tittel",
                          "type" : "Jobb jeg har nå",
                          "status" : "Avbrutt",
                          "detaljer" : [ {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Fra dato",
                            "tekst": "${jobbAktivitetAvbrutt.fraDato.norskDato()}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Til dato",
                            "tekst": "${jobbAktivitetAvbrutt.tilDato.norskDato()}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Stillingsandel",
                            "tekst" : "HELTID"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Arbeidsgiver",
                            "tekst" : "Vikar"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Ansettelsesforhold",
                            "tekst" : "7,5 timer"
                          }, {
                            "stil" : "PARAGRAF",
                            "tittel" : "Beskrivelse",
                            "tekst" : "beskrivelse"
                          } ],
                          "meldinger" : [ ]
                        } ]
                      },
                      "dialogTråder" : [ {
                        "overskrift" : "Penger",
                        "meldinger" : [ {
                          "avsender" : "BRUKER",
                          "sendt" : "05 februar 2024 kl. 14:29",
                          "lest" : true,
                          "viktig" : false,
                          "tekst" : "Jeg liker NAV. NAV er snille!"
                        } ],
                        "egenskaper" : [ ]
                      } ]
                    }
                """.trimIndent())
            ))
    }

    private fun stubDialogTråder(fnr: String, oppfølgingsperiode: String, aktivitetId: String) {
        stubFor(
            get(
                urlEqualTo(
                    "/veilarbdialog/api/dialog?fnr=$fnr"
                )
            )
                .willReturn(
                    aResponse().withBody(
                        """
                            [
                                {
                                    "id": "618055",
                                    "aktivitetId": "$aktivitetId",
                                    "overskrift": "Arbeidsmarkedsopplæring (Gruppe): Kurs: Speiderkurs gruppe-AMO",
                                    "sisteTekst": "Jada",
                                    "sisteDato": "2024-02-05T13:31:22.238+00:00",
                                    "opprettetDato": "2024-02-05T13:31:11.564+00:00",
                                    "historisk": false,
                                    "lest": true,
                                    "venterPaSvar": false,
                                    "ferdigBehandlet": false,
                                    "lestAvBrukerTidspunkt": "2024-02-05T13:31:19.382+00:00",
                                    "erLestAvBruker": true,
                                    "oppfolgingsperiode": "$oppfølgingsperiode",
                                    "henvendelser": [
                                        {
                                            "id": "1147416",
                                            "dialogId": "618057",
                                            "avsender": "VEILEDER",
                                            "avsenderId": "Z994188",
                                            "sendt": "2024-02-05T13:31:11.588+00:00",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "wehfuiehwf\n\nHilsen F_994188 E_994188"
                                        },
                                        {
                                            "id": "1147417",
                                            "dialogId": "618057",
                                            "avsender": "BRUKER",
                                            "avsenderId": "$fnr",
                                            "sendt": "2024-02-05T13:31:22.238+00:00",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "Jada"
                                        }
                                    ],
                                    "egenskaper": []
                                },
                                {
                                    "id": "618056",
                                    "aktivitetId": null,
                                    "overskrift": "Penger",
                                    "sisteTekst": "Jeg liker NAV. NAV er snille!",
                                    "sisteDato": "2024-02-05T13:29:18.635+00:00",
                                    "opprettetDato": "2024-02-05T13:29:18.616+00:00",
                                    "historisk": false,
                                    "lest": true,
                                    "venterPaSvar": false,
                                    "ferdigBehandlet": false,
                                    "lestAvBrukerTidspunkt": null,
                                    "erLestAvBruker": true,
                                    "oppfolgingsperiode": "$oppfølgingsperiode",
                                    "henvendelser": [
                                        {
                                            "id": "1147415",
                                            "dialogId": "618056",
                                            "avsender": "BRUKER",
                                            "avsenderId": "$fnr",
                                            "sendt": "2024-02-05T13:29:18.635+00:00",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "Jeg liker NAV. NAV er snille!"
                                        }
                                    ],
                                    "egenskaper": []
                                }
                            ]
                        """.trimIndent()
                    )
                )
        )
    }
}
