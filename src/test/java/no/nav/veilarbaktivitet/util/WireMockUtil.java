package no.nav.veilarbaktivitet.util;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import no.nav.common.types.identer.EksternBrukerId;

import java.util.Arrays;

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

        stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderOppfolging\":" + underOppfolging + "}")));

        stubFor(get("/veilarboppfolging/api/v2/manuell/status?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderManuellOppfolging\":" + erManuell + ",\"krrStatus\":{\"kanVarsles\":" + kanVarsles + ",\"erReservert\":" + erReservertKrr + "}}")));

        if (erUnderKvp) {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody("{\"enhet\":\"9999\"}")));
        } else {
            stubFor(get("/veilarboppfolging/api/v2/kvp?aktorId=" + aktorId)
                    .willReturn(aResponse().withStatus(204)));
        }

        stubFor(get("/aktorTjeneste/identer?gjeldende=true&identgruppe=AktoerId")
            .withHeader("Nav-Personidenter", equalTo(fnr))
                .willReturn(ok().withBody(
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

        stubFor(get("/veilarbperson/api/person/" + fnr + "/harNivaa4")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"harbruktnivaa4\":" + harBruktNivaa4 + "}")));

    }
}
