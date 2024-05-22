package no.nav.veilarbaktivitet.arkivering

import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortUtil
import no.nav.veilarbaktivitet.aktivitetskort.ArenaKort
import no.nav.veilarbaktivitet.aktivitetskort.arenaMeldingHeaders
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType.*
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeType
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class ArkiveringsControllerTest : SpringBootTestBase() {

    @Test
    fun `Når man ber om forhåndsvist pdf skal man sende data til orkivar og returnere resultat`() {
        // Given
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")

        val sisteOppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }

        val jobbAktivitetPlanlegger = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiodeId).build()
        jobbAktivitetPlanlegger.status = AktivitetStatus.PLANLAGT
        val opprettetJobbAktivitetPlanlegger = aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitetPlanlegger)

        val jobbAktivitetAvbrutt = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiodeId).build()
        jobbAktivitetAvbrutt.status = AktivitetStatus.AVBRUTT
        val opprettetJobbAktivitetAvbrutt = aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitetAvbrutt)

        val oppfølgingsperiodeId = sisteOppfølgingsperiode.oppfolgingsperiodeId.toString()
        val meldingerSendtTidspunktUtc = "2024-02-05T13:31:22.238+00:00"
        stubDialogTråder(
            fnr = bruker.fnr,
            oppfølgingsperiodeId = oppfølgingsperiodeId,
            aktivitetId = opprettetJobbAktivitetPlanlegger.id,
            meldingerSendtTidspunkt = meldingerSendtTidspunktUtc
        )
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl =
            "http://localhost:$port/veilarbaktivitet/api/arkivering/forhaandsvisning?oppfolgingsperiodeId=$oppfølgingsperiodeId"

        // When
        val forhaandsvisning = veileder
            .createRequest(bruker)
            .get(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response()
            .`as`(ArkiveringsController.ForhaandsvisningOutboundDTO::class.java)

        // Then
        assertThat(forhaandsvisning.forhaandsvisningOpprettet).isCloseTo(
            ZonedDateTime.now(),
            within(500, ChronoUnit.MILLIS)
        )

        val meldingerSendtTidspunkt = ZonedDateTime.parse(meldingerSendtTidspunktUtc)
        val expectedMeldingerSendtNorskTid = "${norskDato(meldingerSendtTidspunkt)} kl. ${klokkeslett(meldingerSendtTidspunkt)}"

        verify(
            exactly(1), postRequestedFor(urlEqualTo("/orkivar/forhaandsvisning"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(
                    equalToJson(
                        """
                    {
                      "navn": "${bruker.navn.tilFornavnMellomnavnEtternavn()}",
                      "fnr": "${bruker.fnr}",
                      "oppfølgingsperiodeStart": "${norskDato(sisteOppfølgingsperiode.startTid)}",
                      "oppfølgingsperiodeSlutt": ${sisteOppfølgingsperiode.sluttTid?.let { norskDato(it) } ?: null},
                      "oppfølgingsperiodeId": "${sisteOppfølgingsperiode.oppfolgingsperiodeId}",
                      "aktiviteter" : {
                        "Planlagt" : [ {
                          "tittel" : "tittel",
                          "type" : "Jobb jeg har nå",
                          "status" : "Planlagt",
                          "detaljer" : [ {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Fra dato",
                            "tekst": "${norskDato(jobbAktivitetPlanlegger.fraDato)}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Til dato",
                            "tekst": "${norskDato(jobbAktivitetPlanlegger.tilDato)}"
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
                          "dialogtråd": {
                              "overskrift" : "Arbeidsmarkedsopplæring (Gruppe): Kurs: Speiderkurs gruppe-AMO",
                              "meldinger" : [ {
                                "avsender" : "VEILEDER",
                                "sendt" : "$expectedMeldingerSendtNorskTid",
                                "lest" : true,
                                "viktig" : false,
                                "tekst" : "wehfuiehwf\n\nHilsen F_994188 E_994188"
                              } ],
                              "egenskaper" : [ ],
                              "indexSisteMeldingLestAvBruker" : null,
                              "tidspunktSistLestAvBruker" : null
                          },
                          "etiketter": [],
                            "eksterneHandlinger" : [ ],
                            "historikk" : {
                              "endringer" : [ {
                              "formattertTidspunkt" : "${norskDato(opprettetJobbAktivitetPlanlegger.endretDato)} kl. ${klokkeslett(opprettetJobbAktivitetPlanlegger.endretDato)}",
                              "beskrivelse" : "Bruker opprettet aktiviteten"
                              } ]  
                            }
                        } ],
                        "Avbrutt" : [ {
                          "tittel" : "tittel",
                          "type" : "Jobb jeg har nå",
                          "status" : "Avbrutt",
                          "detaljer" : [ {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Fra dato",
                            "tekst": "${norskDato(opprettetJobbAktivitetAvbrutt.fraDato)}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Til dato",
                            "tekst": "${norskDato(opprettetJobbAktivitetAvbrutt.tilDato)}"
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
                          "dialogtråd" : null,
                          "etiketter": [],
                          "eksterneHandlinger" : [],
                          "historikk" : {
                            "endringer" : [ {
                              "formattertTidspunkt" : "${norskDato(opprettetJobbAktivitetAvbrutt.endretDato)} kl. ${klokkeslett(opprettetJobbAktivitetAvbrutt.endretDato)}",
                              "beskrivelse" : "Bruker opprettet aktiviteten"
                            } ]
                          }
                        } ]
                      },
                      "dialogtråder" : [ {
                        "overskrift" : "Penger",
                        "meldinger" : [ {
                          "avsender" : "BRUKER",
                          "sendt" : "$expectedMeldingerSendtNorskTid",
                          "lest" : true,
                          "viktig" : false,
                          "tekst" : "Jeg liker NAV. NAV er snille!"
                        } ],
                        "egenskaper" : [ ],
                        "indexSisteMeldingLestAvBruker" : null,
                        "tidspunktSistLestAvBruker" : null
                      } ],
                      "mål": "${bruker.brukerOptions.mål}"
                    }
                """.trimIndent()
                    )
                )
        )
    }

    @Test
    fun `Når man skal journalføre sender man data til orkivar`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val journalførendeEnhet = "0909"
        val sisteOppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }

        val jobbAktivitetPlanlegger = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiodeId).build()
        jobbAktivitetPlanlegger.status = AktivitetStatus.PLANLAGT
        val opprettetJobbAktivitet = aktivitetTestService.opprettAktivitet(bruker, bruker, jobbAktivitetPlanlegger)

        val oppfølgingsperiodeId = sisteOppfølgingsperiode.oppfolgingsperiodeId.toString()

        val meldingerSendtTidspunktUtc = "2024-02-05T13:31:22.238+00:00"
        stubDialogTråder(
            fnr = bruker.fnr,
            oppfølgingsperiodeId = oppfølgingsperiodeId,
            aktivitetId = opprettetJobbAktivitet.id,
            meldingerSendtTidspunkt = meldingerSendtTidspunktUtc
        )
        stubIngenArenaAktiviteter(bruker.fnr)
        val arkiveringsUrl =
            "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiodeId"

        val body = ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), journalførendeEnhet)
        veileder
            .createRequest(bruker)
            .body(body)
            .post(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())

        val meldingerSendtTidspunkt = ZonedDateTime.parse(meldingerSendtTidspunktUtc)
        val expectedMeldingerSendtNorskTid = "${norskDato(meldingerSendtTidspunkt)} kl. ${klokkeslett(meldingerSendtTidspunkt)}"
        verify(
            exactly(1), postRequestedFor(urlEqualTo("/orkivar/arkiver"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(
                    equalToJson(
                        """
                    {
                      "navn": "${bruker.navn.tilFornavnMellomnavnEtternavn()}",
                      "fnr": "${bruker.fnr}",
                      "oppfølgingsperiodeStart": "${norskDato(sisteOppfølgingsperiode.startTid)}",
                      "oppfølgingsperiodeSlutt": ${sisteOppfølgingsperiode?.sluttTid?.let { norskDato(it) } ?: null},
                      "sakId": ${bruker.sakId},
                      "fagsaksystem": "ARBEIDSOPPFOLGING",
                      "tema": "OPP",
                      "oppfølgingsperiodeId": "${oppfølgingsperiodeId}",
                      "journalførendeEnhet": "$journalførendeEnhet",
                      "aktiviteter" : {
                        "Planlagt" : [ {
                          "tittel" : "tittel",
                          "type" : "Jobb jeg har nå",
                          "status" : "Planlagt",
                          "detaljer" : [ {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Fra dato",
                            "tekst": "${norskDato(jobbAktivitetPlanlegger.fraDato)}"
                          }, {
                            "stil" : "HALV_LINJE",
                            "tittel" : "Til dato",
                            "tekst": "${norskDato(jobbAktivitetPlanlegger.tilDato)}"
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
                        "dialogtråd" : {
                          "overskrift" : "Arbeidsmarkedsopplæring (Gruppe): Kurs: Speiderkurs gruppe-AMO",
                          "meldinger" : [ {
                            "avsender" : "VEILEDER",
                            "sendt" : "$expectedMeldingerSendtNorskTid",
                            "lest" : true,
                            "viktig" : false,
                            "tekst" : "wehfuiehwf\n\nHilsen F_994188 E_994188"
                          }],
                          "egenskaper" : [ ],
                          "indexSisteMeldingLestAvBruker" : null,
                          "tidspunktSistLestAvBruker" : null
                        },
                          "etiketter": [],
                          "eksterneHandlinger" : [ ],
                          "historikk" : {
                            "endringer" : [ {
                              "formattertTidspunkt" : "${norskDato(opprettetJobbAktivitet.endretDato)} kl. ${klokkeslett(opprettetJobbAktivitet.endretDato)}",
                              "beskrivelse" : "Bruker opprettet aktiviteten"
                            } ]
                          }
                        } ]
                      },
                      "dialogtråder" : [ {
                        "overskrift" : "Penger",
                        "meldinger" : [ {
                          "avsender" : "BRUKER",
                          "sendt" : "$expectedMeldingerSendtNorskTid",
                          "lest" : true,
                          "viktig" : false,
                          "tekst" : "Jeg liker NAV. NAV er snille!"
                        } ],
                        "egenskaper" : [ ],
                        "indexSisteMeldingLestAvBruker" : null,
                        "tidspunktSistLestAvBruker" : null
                      } ],
                      "mål": "${bruker.brukerOptions.mål}"
                    }
                """.trimIndent()
                    )
                )
        )
    }

    @Test
    fun `Når man forhåndsviser PDF skal kun riktig oppfølgingsperiode være inkludert`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiodeForArkivering = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val annenOppfølgingsperiode = UUID.randomUUID()
        val aktivititetIAnnenOppfolgingsperiode = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(annenOppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, bruker, aktivititetIAnnenOppfolgingsperiode)
        stubDialogTråder(
            fnr = bruker.fnr,
            oppfølgingsperiodeId = annenOppfølgingsperiode.toString(),
            aktivitetId = "dummy"
        )
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl =
            "http://localhost:$port/veilarbaktivitet/api/arkivering/forhaandsvisning?oppfolgingsperiodeId=$oppfølgingsperiodeForArkivering&journalforendeEnhet=0909"
        veileder
            .createRequest(bruker)
            .get(arkiveringsUrl)

        val journalforingsrequest =
            getAllServeEvents().filter { it.request.url.contains("orkivar/forhaandsvisning") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ForhåndsvisningPayload::class.java)
        assertThat(arkivPayload.aktiviteter).isEmpty()
        assertThat(arkivPayload.dialogtråder).isEmpty()
    }

    @Test
    fun `Når man journalfører skal kun riktig oppfølgingsperiode være inkludert`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiodeForArkivering = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val annenOppfølgingsperiode = UUID.randomUUID()
        val aktivititetIAnnenOppfolgingsperiode = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(annenOppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, bruker, aktivititetIAnnenOppfolgingsperiode)
        stubDialogTråder(
            fnr = bruker.fnr,
            oppfølgingsperiodeId = annenOppfølgingsperiode.toString(),
            aktivitetId = "dummy"
        )
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl =
            "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiodeForArkivering"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        assertThat(arkivPayload.aktiviteter).isEmpty()
        assertThat(arkivPayload.dialogtråder).isEmpty()
    }

    @Test
    fun `Når man journalfører på bruker i KVP skal aktiviteter med kontorsperre ekskluderes`() {
        val (kvpBruker, veileder) = hentKvpBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = kvpBruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        // Aktiviteten vil få satt kontorsperre fordi bruker er under KVP
        val kvpAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(kvpBruker, veileder, kvpAktivitet)

        stubDialogTråder(kvpBruker.fnr, oppfølgingsperiode.toString(),"dummyAktivitetId")
        stubIngenArenaAktiviteter(kvpBruker.fnr)
        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"

        veileder
            .createRequest(kvpBruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        assertThat(arkivPayload.aktiviteter).isEmpty()
    }

    @Test
    fun `Når man journalfører på bruker som har vært i KVP skal aktiviteter utenom KVP-perioden inkluderes`() {
        val (bruker, veileder) = hentKvpBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val kvpAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, kvpAktivitet)
        navMockService.updateBruker(bruker, bruker.getBrukerOptions().toBuilder().erUnderKvp(false).build())
        val ikkeKvpAktivitetTittel = "IkkeKvpAktivitet"
        val ikkeKvpAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).tittel(ikkeKvpAktivitetTittel).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, ikkeKvpAktivitet)
        stubIngenArenaAktiviteter(bruker.fnr)
       stubDialogTråder(bruker.fnr, oppfølgingsperiode.toString(),"dummyAktivitetId")

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        assertThat(arkivPayload.aktiviteter).hasSize(1)
        assertThat(arkivPayload.aktiviteter.values.flatten().first().tittel).isEqualTo(ikkeKvpAktivitetTittel)
    }

    @Test
    fun `Når man journalfører skal eksterne aktiviteter inkluderes`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val eksternaAktiviteterTyper = AktivitetskortType.values().filter { it != ARENA_TILTAK }
        aktivitetTestService.opprettEksterntAktivitetsKort(
            eksternaAktiviteterTyper.map { aktivitetskortType ->
                KafkaAktivitetskortWrapperDTO(
                    aktivitetskortType = aktivitetskortType,
                    aktivitetskort = AktivitetskortUtil.ny(
                        UUID.randomUUID(),
                        AktivitetskortStatus.PLANLAGT,
                        ZonedDateTime.now(),
                        bruker
                    ),
                    source = "source",
                    messageId = UUID.randomUUID())
            }
        )
        stubDialogTråder(bruker.fnr, UUID.randomUUID().toString(),"dummy")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val aktiviteterSendtTilArkiv = arkivPayload.aktiviteter.values.flatten()
        assertThat(aktiviteterSendtTilArkiv).hasSize(eksternaAktiviteterTyper.size)
    }

    @Test
    fun `Når man journalfører skal migrerte Arena-aktiviteter inkluderes`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(
            KafkaAktivitetskortWrapperDTO(
                aktivitetskortType = ARENA_TILTAK,
                aktivitetskort = AktivitetskortUtil.ny(
                    UUID.randomUUID(),
                    AktivitetskortStatus.PLANLAGT,
                    ZonedDateTime.now(),
                    bruker
                ),
                source = "source",
                messageId = UUID.randomUUID()), arenaMeldingHeaders(bruker)
        ))
        stubDialogTråder(bruker.fnr, UUID.randomUUID().toString(),"dummy")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val aktiviteterSendtTilArkiv = arkivPayload.aktiviteter.values.flatten()
        assertThat(aktiviteterSendtTilArkiv).hasSize(1)
    }

    @Test
    fun `Når man journalfører eksterne aktiviteter skal kun handlinger med INTERN og FELLES lenketype inkluderes`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val eksternHandling = LenkeSeksjon("EksternHandlingTekst", "EksternHandlingSubTekst", URL("http://localhost:8080"), LenkeType.EKSTERN)
        val internHandling = LenkeSeksjon("InternHandlingTekst", "InternHandlingSubTekst", URL("http://localhost:8080"), LenkeType.INTERN)
        val fellesHandling = LenkeSeksjon("FellesHandlingTekst", "FellesHandlingSubTekst", URL("http://localhost:8080"), LenkeType.FELLES)

        val eksternAktivitetskort = KafkaAktivitetskortWrapperDTO(
            aktivitetskortType = MIDLERTIDIG_LONNSTILSKUDD,
            aktivitetskort = AktivitetskortUtil.ny(
                UUID.randomUUID(),
                AktivitetskortStatus.PLANLAGT,
                ZonedDateTime.now(),
                bruker
            ).copy(handlinger = listOf(eksternHandling, internHandling, fellesHandling)),
            source = "source",
            messageId = UUID.randomUUID(),
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(eksternAktivitetskort))
        stubDialogTråder(bruker.fnr, UUID.randomUUID().toString(),"dummy")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val aktivitetSendtTilArkiv = arkivPayload.aktiviteter.values.flatten().first()
        assertThat(aktivitetSendtTilArkiv.eksterneHandlinger).hasSize(2)
        assertThat(aktivitetSendtTilArkiv.eksterneHandlinger).containsExactlyInAnyOrder(
            EksternHandling(internHandling.tekst, internHandling.subtekst, internHandling.url.toString()),
            EksternHandling(fellesHandling.tekst, fellesHandling.subtekst, fellesHandling.url.toString())
        )
    }

    @Test
    fun `SamtalereferatAktivitet som ikke er delt med bruker ignoreres`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val referatPublisertTittel = "Referat er publisert"
        val samtalereferatIkkeDelt = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT).setErReferatPublisert(false)
            .toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, samtalereferatIkkeDelt)
        val samtaleReferatDelt = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT).setErReferatPublisert(true)
            .setTittel(referatPublisertTittel).toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, samtaleReferatDelt)
            .setTittel(referatPublisertTittel).toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        stubDialogTråder(bruker.fnr, oppfølgingsperiode.toString(),"dummyAktivitetId")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val aktiviteter = arkivPayload.aktiviteter.flatMap { it.value }
        assertThat(aktiviteter).hasSize(1)
        assertThat(aktiviteter.first().tittel).isEqualTo(referatPublisertTittel)
    }

    @Test
    fun `Journalfør MøteAktivitet men ikke referatet når det ikke er delt med bruker`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val møteAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(false)
            .toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, møteAktivitet)
        stubDialogTråder(bruker.fnr, oppfølgingsperiode.toString(),"dummyAktivitetId")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val journalførtAktivitet = arkivPayload.aktiviteter.flatMap { it.value }.first()
        assertThat(journalførtAktivitet.detaljer.find { it.tittel == "Samtalereferat" }?.tekst).isEmpty()
    }

    @Test
    fun `Journalfør MøteAktivitet-kort med referatet når det er delt med bruker`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val referat = "Dette er et referat"
        val møteAktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(true)
            .setReferat(referat).toBuilder().oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, veileder, møteAktivitet)
        stubDialogTråder(bruker.fnr, oppfølgingsperiode.toString(),"dummyAktivitetId")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        val journalforingsrequest = getAllServeEvents().filter { it.request.url.contains("orkivar/arkiver") }.first()
        val arkivPayload = JsonUtils.fromJson(journalforingsrequest.request.bodyAsString, ArkivPayload::class.java)
        val journalførtAktivitet = arkivPayload.aktiviteter.flatMap { it.value }.first()
        assertThat(journalførtAktivitet.detaljer.find { it.tittel == "Samtalereferat" }?.tekst).isEqualTo(referat)
    }

    @Test
    fun `Ikke-migrerte Arena-aktiviteter skal bli journalført`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }
        val arenaAktivitetEndretDato = iso8601DateFromZonedDateTime(oppfølgingsperiode.startTid.plusDays(1),  ZoneId.systemDefault())
        val arenaAktivitetId = "ARENATA123"
        val tiltaksnavn = "Et tiltaksnavn fra Arena!"
        stubHentArenaAktiviteter(bruker.fnr, arenaAktivitetId, arenaAktivitetEndretDato, tiltaksnavn)
        stubIngenDialogTråder(bruker.fnr)
        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=${oppfølgingsperiode.oppfolgingsperiodeId}"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(ZonedDateTime.now(), "dummyEnhet"))
            .post(arkiveringsUrl)

        verify(
            exactly(1), postRequestedFor(urlEqualTo("/orkivar/arkiver"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withRequestBody(
                    equalToJson(
                        """
                            {
                              "navn" : "Sølvi Normalbakke",
                              "fnr" : "${bruker.fnr}",
                              "oppfølgingsperiodeStart" : "${norskDato(oppfølgingsperiode.startTid)}",
                              "oppfølgingsperiodeSlutt" : null,
                              "sakId" : 1000,
                              "fagsaksystem" : "ARBEIDSOPPFOLGING",
                              "tema" : "OPP",
                              "oppfølgingsperiodeId" : "${oppfølgingsperiode.oppfolgingsperiodeId}",
                              "journalførendeEnhet" : "dummyEnhet",
                              "aktiviteter" : {
                                "Gjennomføres" : [ {
                                  "tittel" : "Et tiltaksnavn fra Arena!",
                                  "type" : "Tiltak gjennom NAV",
                                  "status" : "Gjennomføres",
                                  "detaljer" : [ {
                                    "stil" : "PARAGRAF",
                                    "tittel" : "Fullført / Tiltak gjennom NAV",
                                    "tekst" : null
                                  }, {
                                    "stil" : "HALV_LINJE",
                                    "tittel" : "Fra dato",
                                    "tekst" : "18. november 2021"
                                  }, {
                                    "stil" : "HALV_LINJE",
                                    "tittel" : "Til dato",
                                    "tekst" : "25. november 2021"
                                  }, {
                                    "stil" : "HALV_LINJE",
                                    "tittel" : "Deltakelsesprosent",
                                    "tekst" : "60%"
                                  }, {
                                    "stil" : "HALV_LINJE",
                                    "tittel" : "Arrangør",
                                    "tekst" : "arrangor"
                                  }, {
                                    "stil" : "HALV_LINJE",
                                    "tittel" : "Dager per uke",
                                    "tekst" : "3"
                                  } ],
                                  "dialogtråd" : null,
                                  "etiketter" : [ {
                                    "stil" : "AVTALT",
                                    "tekst" : "Avtalt med NAV"
                                  } ],
                                  "eksterneHandlinger" : [ ],
                                  "historikk" : {
                                    "endringer" : [ ]
                                  }
                                } ]
                              },
                              "dialogtråder" : [],
                              "mål" : "Å få meg jobb"
                            }
                        """.trimIndent()
                    )
                )
        )
    }

    @Test
    fun `Kast 409 Conflict når man arkiverer dersom ny data har kommet etter forhåndsvisningen`() {
        val (bruker, veileder) = hentBrukerOgVeileder("Sølvi", "Normalbakke")
        val oppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }.oppfolgingsperiodeId
        val forhaandsvisningstidspunkt = ZonedDateTime.now().minusSeconds(1)
        val aktivitetSistEndret = Date()
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.IJOBB)
            .toBuilder()
            .endretDato(aktivitetSistEndret)
            .oppfolgingsperiodeId(oppfølgingsperiode).build()
        aktivitetTestService.opprettAktivitet(bruker, bruker, aktivitet)
        stubDialogTråder(fnr = bruker.fnr, oppfølgingsperiodeId = oppfølgingsperiode.toString(), aktivitetId = "dummy")
        stubIngenArenaAktiviteter(bruker.fnr)

        val arkiveringsUrl =
            "http://localhost:$port/veilarbaktivitet/api/arkivering/journalfor?oppfolgingsperiodeId=$oppfølgingsperiode"
        veileder
            .createRequest(bruker)
            .body(ArkiveringsController.ArkiverInboundDTO(forhaandsvisningstidspunkt, "dummyEnhet"))
            .post(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.CONFLICT.value())
    }

    @Test
    fun `Kall mot arkiveringsendepunkt kaster 403 når oppfølgingsperiodeId mangler`() {
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(bruker)

        val arkiveringsUrl = "http://localhost:$port/veilarbaktivitet/api/arkivering/forhaandsvisning"

        veileder
            .createRequest(bruker)
            .get(arkiveringsUrl)
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    private fun stubDialogTråder(fnr: String, oppfølgingsperiodeId: String, aktivitetId: String, meldingerSendtTidspunkt: String = "2024-02-05T13:31:22.238+00:00") {
        stubFor(
            get(
                urlEqualTo(
                    "/veilarbdialog/api/dialog?fnr=$fnr&ekskluderDialogerMedKontorsperre=true"
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
                                    "lestAvBrukerTidspunkt": "2024-03-05T13:31:19.382+00:00",
                                    "erLestAvBruker": true,
                                    "oppfolgingsperiode": "$oppfølgingsperiodeId",
                                    "henvendelser": [
                                        {
                                            "id": "1147416",
                                            "dialogId": "618057",
                                            "avsender": "VEILEDER",
                                            "avsenderId": "Z994188",
                                            "sendt": "$meldingerSendtTidspunkt",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "wehfuiehwf\n\nHilsen F_994188 E_994188"
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
                                    "oppfolgingsperiode": "$oppfølgingsperiodeId",
                                    "henvendelser": [
                                        {
                                            "id": "1147415",
                                            "dialogId": "618056",
                                            "avsender": "BRUKER",
                                            "avsenderId": "$fnr",
                                            "sendt": "$meldingerSendtTidspunkt",
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

    private fun stubIngenDialogTråder(fnr: String) {
        stubFor(get(urlEqualTo("/veilarbdialog/api/dialog?fnr=$fnr&ekskluderDialogerMedKontorsperre=true"))
                .willReturn(aResponse().withBody(
                        """
                            []
                        """.trimIndent()
                )
            )
        )
    }

    private fun stubIngenArenaAktiviteter(fnr: String) {
        stubFor(get(urlEqualTo("/veilarbarena/api/arena/aktiviteter?fnr=$fnr")).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {
                      "tiltaksaktiviteter": [],
                      "gruppeaktiviteter": [],
                      "utdanningsaktiviteter": []
                    }
                """.trimIndent())
        ))
    }

    private fun stubHentArenaAktiviteter(fnr: String, arenaAktivitetId: String, sistEndret: String, tiltaksnavn: String) {
        stubFor(get(urlEqualTo("/veilarbarena/api/arena/aktiviteter?fnr=$fnr")).willReturn(
            aResponse().withStatus(200)
                .withBody("""
                    {
                      "tiltaksaktiviteter": [
                          {
                            "tiltaksnavn": "$tiltaksnavn",
                            "aktivitetId": "$arenaAktivitetId",
                            "tiltakLokaltNavn": "lokaltnavn",
                            "arrangor": "arrangor",
                            "bedriftsnummer": "asd",
                            "deltakelsePeriode": {
                                "fom": "2021-11-18",
                                "tom": "2021-11-25"
                            },
                            "deltakelseProsent": 60,
                            "deltakerStatus": "GJENN",
                            "statusSistEndret": "$sistEndret",
                            "begrunnelseInnsoking": "asd",
                            "antallDagerPerUke": 3.0
                          }
                      ],
                      "gruppeaktiviteter": [],
                      "utdanningsaktiviteter": []
                    }
                """.trimIndent())
        ))
    }

    private fun hentBrukerOgVeileder(brukerFornavn: String, brukerEtternavn: String): Pair<MockBruker, MockVeileder> {
        val navn = Navn(brukerFornavn, null, brukerEtternavn)
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().navn(navn).build()
        val bruker = navMockService.createHappyBruker(brukerOptions)
        val veileder = navMockService.createVeileder(bruker)
        return Pair(bruker, veileder)
    }

    private fun hentKvpBrukerOgVeileder(brukerFornavn: String, brukerEtternavn: String): Pair<MockBruker, MockVeileder> {
        val navn = Navn(brukerFornavn, null, brukerEtternavn)
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().erUnderKvp(true).navn(navn).build()
        val bruker = navMockService.createHappyBruker(brukerOptions)
        val veileder = navMockService.createVeileder(bruker)
        return Pair(bruker, veileder)
    }
}
