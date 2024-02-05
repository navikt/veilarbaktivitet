package no.nav.veilarbaktivitet.mock_nav_modell;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.person.Navn;
import no.nav.veilarbaktivitet.person.Person;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockUtil {

    public static final ZonedDateTime GJELDENDE_OPPFOLGINGSPERIODE_MOCK_START = ZonedDateTime.now().minusDays(5);

    static void stubBruker(MockBruker mockBruker) {
        String fnr = mockBruker.getFnr();
        Person.AktorId aktorId = mockBruker.getAktorId();
        boolean erManuell = mockBruker.getBrukerOptions().isErManuell();
        boolean erReservertKrr = mockBruker.getBrukerOptions().isErReservertKrr();
        boolean erUnderKvp = mockBruker.getBrukerOptions().isErUnderKvp();
        boolean kanVarsles = mockBruker.getBrukerOptions().isKanVarsles();
        boolean underOppfolging = mockBruker.getBrukerOptions().isUnderOppfolging();
        boolean harBruktNivaa4 = mockBruker.getBrukerOptions().isHarBruktNivaa4();
        Navn navn = mockBruker.getBrukerOptions().getNavn();
        String kontorsperreEnhet = mockBruker.getOppfolgingsenhet();

        boolean oppfolgingFeiler = mockBruker.getBrukerOptions().isOppfolgingFeiler();

        oppfolging(fnr, aktorId, underOppfolging, oppfolgingFeiler, mockBruker.getOppfolgingsperiode());
        manuell(fnr, erManuell, erReservertKrr, kanVarsles);
        kvp(aktorId, erUnderKvp, kontorsperreEnhet);
        aktor(fnr, aktorId);
        nivaa4(fnr, harBruktNivaa4);
        hentPerson(fnr, navn);
        hentDialogTråder(fnr);
        arkivering();
    }

    private static void oppfolging(String fnr, Person.AktorId aktorId, boolean underOppfolging, boolean oppfolgingFeiler, UUID periode) {
        if (oppfolgingFeiler) {
            stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId.get())
                    .willReturn(aResponse().withStatus(500)));
            return;
        }
        stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderOppfolging\":" + underOppfolging + "}")));

        if (underOppfolging) {
            OppfolgingPeriodeMinimalDTO oppfolgingsperiode = new OppfolgingPeriodeMinimalDTO(
                    periode,
                    GJELDENDE_OPPFOLGINGSPERIODE_MOCK_START,
                    null
            );
            OppfolgingPeriodeMinimalDTO gammelPeriode = new OppfolgingPeriodeMinimalDTO(
                    UUID.randomUUID(),
                    ZonedDateTime.now().minusDays(100),
                    ZonedDateTime.now().minusDays(50)
            );

            String gjeldendePeriode = JsonUtils.toJson(oppfolgingsperiode);

            String oppfolgingsperioder = JsonUtils.toJson(List.of(oppfolgingsperiode, gammelPeriode));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(gjeldendePeriode)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId.get())
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(oppfolgingsperioder)));

        } else {
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(204)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId.get())
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(JsonUtils.toJson(Collections.emptyList()))));
        }
    }

    private static void nivaa4(String fnr, boolean harBruktNivaa4) {
        stubFor(get("/veilarbperson/api/person/" + fnr + "/harNivaa4")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"harbruktnivaa4\":" + harBruktNivaa4 + "}")));
    }

    private static void manuell(String fnr, boolean erManuell, boolean erReservertKrr, boolean kanVarsles) {
        stubFor(get("/veilarboppfolging/api/v2/manuell/status?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderManuellOppfolging\":" + erManuell + ",\"krrStatus\":{\"kanVarsles\":" + kanVarsles + ",\"erReservert\":" + erReservertKrr + "}}")));
    }

    private static void kvp(Person.AktorId aktorId, boolean erUnderKvp, String enhet) {
        if (erUnderKvp) {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId.get())
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody("{\"enhet\":\"" + enhet + "\"}")));
        } else {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId.get())
                    .willReturn(aResponse().withStatus(204)));
        }
    }

    public static void aktorUtenGjeldende(String fnr, Person.AktorId aktorId) {
        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId.get())))
                .willReturn(aResponse()
                        .withBody("""
                                {
                                  "data": {
                                    "hentIdenter": {
                                      "identer": null
                                    }
                                  },
                                  "errors": [{
                                    "message": "Fant ikke person"
                                  }]
                                }
                                """)));

        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*AKTORID.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fnr)))
                .willReturn(aResponse()
                        .withBody("""
                                {
                                  "data": {
                                    "hentIdenter": {
                                      "identer": null
                                    }
                                  },
                                  "errors": [{
                                    "message": "Fant ikke person"
                                  }]
                                }
                                """)));
    }

    private static void aktor(String fnr, Person.AktorId aktorId) {

        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId.get())))
                .willReturn(aResponse()
                        .withBody("""
                                {
                                  "data": {
                                    "hentIdenter": {
                                      "identer": [{
                                         "ident": "%s",
                                         "historisk": false,
                                         "gruppe": "FOLKEREGISTERIDENT"
                                      }
                                      ]
                                    }
                                  }
                                }
                                """.formatted(fnr))));

        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*AKTORID.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fnr)))
                .willReturn(aResponse()
                        .withBody("""
                                {
                                  "data": {
                                    "hentIdenter": {
                                      "identer": [{
                                         "ident": "%s",
                                         "historisk": false,
                                         "gruppe": "AKTORID"
                                         
                                      }]
                                    }
                                  }
                                }
                                """.formatted(aktorId.get()))));
    }

    private static void hentPerson(String fnr, Navn navn) {
        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*hentPerson.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(fnr)))
                .willReturn(aResponse()
                        .withBody("""
                            {
                              "data": {
                                "hentPerson": {
                                  "navn": [
                                    {
                                      "fornavn": "%s",
                                      "mellomnavn": %s,
                                      "etternavn": "%s"
                                    }
                                  ]
                                }
                              }
                            }
                            """.formatted(navn.getFornavn(), navn.getMellomnavn(), navn.getEtternavn()))));
    }

    private static void hentDialogTråder(String fnr) {
        stubFor(get(urlEqualTo("/veilarbdialog/api/dialog?fnr=" + fnr))
                .willReturn(aResponse().withBody("""
                            [
                                {
                                    "id": "618057",
                                    "aktivitetId": "943688",
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
                                    "oppfolgingsperiode": "dfb70832-485a-4f1b-a1e2-6a4d2c260983",
                                    "henvendelser": [
                                        {
                                            "id": "1147416",
                                            "dialogId": "618057",
                                            "avsender": "VEILEDER",
                                            "avsenderId": "Z994188",
                                            "sendt": "2024-02-05T13:31:11.588+00:00",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "wehfuiehwf\\n\\nHilsen F_994188 E_994188"
                                        },
                                        {
                                            "id": "1147417",
                                            "dialogId": "618057",
                                            "avsender": "BRUKER",
                                            "avsenderId": "%s",
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
                                    "sisteTekst": "Jeg trenger penger, da blir jeg ikke trist lenger!",
                                    "sisteDato": "2024-02-05T13:29:18.635+00:00",
                                    "opprettetDato": "2024-02-05T13:29:18.616+00:00",
                                    "historisk": false,
                                    "lest": true,
                                    "venterPaSvar": false,
                                    "ferdigBehandlet": false,
                                    "lestAvBrukerTidspunkt": null,
                                    "erLestAvBruker": true,
                                    "oppfolgingsperiode": "dfb70832-485a-4f1b-a1e2-6a4d2c260983",
                                    "henvendelser": [
                                        {
                                            "id": "1147415",
                                            "dialogId": "618056",
                                            "avsender": "BRUKER",
                                            "avsenderId": "%s",
                                            "sendt": "2024-02-05T13:29:18.635+00:00",
                                            "lest": true,
                                            "viktig": false,
                                            "tekst": "Jeg trenger penger, da blir jeg ikke trist lenger!"
                                        }
                                    ],
                                    "egenskaper": []
                                }
                            ]
                        """.formatted(fnr, fnr)
                ))
        );
    }

    private static void arkivering() {
        stubFor(post("/orkivar/arkiver")
                .willReturn(ok())
        );
    }

}