package no.nav.veilarbaktivitet.mock_nav_modell;

import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockUtil {

    static void stubBruker(MockBruker mockBruker) {
        String fnr = mockBruker.getFnr();
        String aktorId = mockBruker.getAktorId();
        boolean erManuell = mockBruker.getBrukerOptions().isErManuell();
        boolean erReservertKrr = mockBruker.getBrukerOptions().isErReservertKrr();
        boolean erUnderKvp = mockBruker.getBrukerOptions().isErUnderKvp();
        boolean kanVarsles = mockBruker.getBrukerOptions().isKanVarsles();
        boolean underOppfolging = mockBruker.getBrukerOptions().isUnderOppfolging();
        boolean harBruktNivaa4 = mockBruker.getBrukerOptions().isHarBruktNivaa4();
        String kontorsperreEnhet = mockBruker.getBrukerOptions().getKontorsperreEnhet();

        boolean oppfolgingFeiler = mockBruker.getBrukerOptions().isOppfolgingFeiler();

        oppfolging(fnr, aktorId, underOppfolging, oppfolgingFeiler, mockBruker.getOppfolgingsperiode());
        manuell(fnr, erManuell, erReservertKrr, kanVarsles);
        kvp(aktorId, erUnderKvp, kontorsperreEnhet);
        aktor(fnr, aktorId);
        nivaa4(fnr, harBruktNivaa4);
    }

    private static void oppfolging(String fnr, String aktorId, boolean underOppfolging, boolean oppfolgingFeiler, UUID periode) {
        if (oppfolgingFeiler) {
            stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId)
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
                    ZonedDateTime.now().minusDays(5),
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
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(oppfolgingsperioder)));

        } else {
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(204)));
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

    private static void kvp(String aktorId, boolean erUnderKvp, String enhet) {
        if (erUnderKvp) {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody("{\"enhet\":\"" + enhet + "\"}")));
        } else {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(aResponse().withStatus(204)));
        }
    }

    public static void aktorUtenGjeldende(String fnr, String aktorId) {
        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId)))
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

    private static void aktor(String fnr, String aktorId) {

        stubFor(post(urlEqualTo("/pdl/graphql"))
                .withRequestBody(matching("^.*FOLKEREGISTERIDENT.*"))
                .withRequestBody(matchingJsonPath("$.variables.ident", equalTo(aktorId)))
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
                                """.formatted(aktorId))));
    }


}
