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

        boolean oppfolgingFeiler = mockBruker.getBrukerOptions().isOppfolgingFeiler();

        oppfolging(fnr, underOppfolging, oppfolgingFeiler, mockBruker.getOppfolgingsperiode());
        manuell(fnr, erManuell, erReservertKrr, kanVarsles);
        kvp(aktorId, erUnderKvp);
        aktor(fnr, aktorId);
        nivaa4(fnr, harBruktNivaa4);
    }

    private static void oppfolging(String fnr, boolean underOppfolging, boolean oppfolgingFeiler, UUID periode) {
        if (oppfolgingFeiler) {
            stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?fnr=" + fnr)
                    .willReturn(aResponse().withStatus(500)));
            return;
        }
        stubFor(get("/veilarboppfolging/api/v2/oppfolging?fnr=" + fnr)
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"erUnderOppfolging\":" + underOppfolging + "}")));

        if (underOppfolging) {
            OppfolgingPeriodeMinimalDTO oppfolgingsperiode = OppfolgingPeriodeMinimalDTO.builder()
                    .startDato(ZonedDateTime.now().minusDays(5))
                    .uuid(periode)
                    .build();
            OppfolgingPeriodeMinimalDTO gammelPeriode = OppfolgingPeriodeMinimalDTO.builder()
                    .startDato(ZonedDateTime.now().minusDays(100))
                    .sluttDato(ZonedDateTime.now().minusDays(50))
                    .uuid(UUID.randomUUID())
                    .build();

            String gjeldendePeriode = JsonUtils.toJson(oppfolgingsperiode);

            String oppfolgingsperioder = JsonUtils.toJson(List.of(oppfolgingsperiode, gammelPeriode));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=" + fnr)
                    .willReturn(ok()
                            .withHeader("Content-Type", "text/json")
                            .withBody(gjeldendePeriode)));
            stubFor(get("/veilarboppfolging/api/v2/oppfolging/perioder?fnr=" + fnr)
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

    public static void aktorUtenGjeldende(String fnr, String aktorId) {
        stubFor(get("/aktorTjeneste/identer?gjeldende=true&identgruppe=AktoerId")
                .withHeader("Nav-Personidenter", equalTo(fnr))
                .willReturn(ok().withBody("" +
                        "{" +
                        "  \"" + fnr + "\": {" +
                        "    \"identer\": [" +
                        "      {" +
                        "        \"ident\": \"" + aktorId + "\"," +
                        "        \"identgruppe\": \"AktoerId\"," +
                        "        \"gjeldende\": false" +
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
                        "        \"gjeldende\": false" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "}")));
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
