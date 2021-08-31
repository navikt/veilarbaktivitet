package no.nav.veilarbaktivitet.util;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;

public class WireMockUtil {

    public static void stubBruker(MockBruker mockBruker) {
        String fnr = mockBruker.getFnr();
        String aktorId = mockBruker.getAktorId();
        boolean erManuell = mockBruker.isErManuell();
        boolean erReservertKrr = mockBruker.isErReservertKrr();
        boolean erUnderKvp = mockBruker.isErUnderKvp();
        boolean kanVarsles = mockBruker.isKanVarsles();
        boolean underOppfolging = mockBruker.isUnderOppfolging();
        boolean harBruktNivaa4 = mockBruker.isHarBruktNivaa4();

        oppfolging(fnr, underOppfolging);
        manuell(fnr, erManuell, erReservertKrr, kanVarsles);
        kvp(aktorId, erUnderKvp);
        aktor(fnr, aktorId);
        nivaa4(fnr, harBruktNivaa4);
    }

    private static void oppfolging(String fnr, boolean underOppfolging) {
        stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderOppfolging\":" + underOppfolging + "}")));
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

    private static void kvp(String aktorId, boolean erUnderKvp) {
        if (erUnderKvp) {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody("{\"enhet\":\"9999\"}")));
        } else {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(aResponse().withStatus(204)));
        }
    }

    private static void aktor(String fnr, String aktorId) {
        stubFor(get("/aktorTjeneste/identer?gjeldende=true&identgruppe=AktoerId")
                .withHeader("Nav-Personidenter", equalTo(fnr))
                .willReturn(ok().withBody("" +
                        "{" +
                        "  \"" + fnr + "\": {" +
                        "    \"identer\": [" +
                        "      {" +
                        "        \"ident\": \"" + aktorId + "\"," +
                        "        \"identgruppe\": \"AktoerId\"," +
                        "        \"gjeldende\": true" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "}")));

        stubFor(get("/aktorTjeneste/identer?gjeldende=true&identgruppe=NorskIdent")
                .withHeader("Nav-Personidenter", equalTo(aktorId))
                .willReturn(ok().withBody("" +
                        "{" +
                        "  \"" + aktorId + "\": {" +
                        "    \"identer\": [" +
                        "      {" +
                        "        \"ident\": \"" + fnr + "\"," +
                        "        \"identgruppe\": \"NorskIdent\"," +
                        "        \"gjeldende\": true" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "}")));
    }


}
