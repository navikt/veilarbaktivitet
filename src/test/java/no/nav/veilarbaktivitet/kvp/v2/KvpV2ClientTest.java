package no.nav.veilarbaktivitet.kvp.v2;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.nav.veilarbaktivitet.person.Person;
import okhttp3.OkHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KvpV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("1234567890");
    private static final String KVP_RESPONS = "kvp/v2/kvpRespons.json";
    private static final String graphqlApiurl = "/veilarboppfolging/graphql";

    private KvpV2Client kvpV2Client;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        kvpV2Client = new KvpV2ClientImpl(
                "http://localhost:" + wireMock.getPort(),
                okHttpClient
        );
        kvpV2Client.setBaseUrl( wireMock.baseUrl());
    }

    @Test
    void test_kvp_ok_response() {
        wireMock.stubFor(post(urlMatching(graphqlApiurl))
                .willReturn(ok()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(KVP_RESPONS)));
        Optional<KontorSperre> kontorSperre = kvpV2Client.get(FNR);

        assertThat(kontorSperre).isNotEmpty();
        assertThat(kontorSperre.get().getEnhetId().get()).isEqualTo("1234");
    }

    @Test
    void test_kvp_kall_feiler() {
        wireMock.stubFor(post(urlMatching(graphqlApiurl))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> kvpV2Client.get(FNR));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Feil ved henting av kontorsperreenhet fra veilarboppfolging (http status: 400)"));
    }

    @Test
    void test_kvp_kall_feiler_i_graphql() {
        wireMock.stubFor(post(urlMatching(graphqlApiurl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                          {
                            "data": null,
                            "errors": [{ "message": "Noe gikk galt" }]
                          }
                        """)
                )
        );
        Exception exception = assertThrows(ResponseStatusException.class, () -> kvpV2Client.get(FNR));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Feil ved henting av kontorsperreenhet fra veilarboppfolging (error i graphql response): [GraphqlError(message=Noe gikk galt, locations=null, path=null, extensions=null)]"));
    }

    @Test
    void test_kvp_httpcode_204() {
        wireMock.stubFor(post(urlMatching(graphqlApiurl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            { "data": { "brukerStatus": { "kontorSperre": null } }, "errors": null }
                        """)));
        Optional<KontorSperre> kvpV2DTO = kvpV2Client.get(FNR);
        assertThat(kvpV2DTO).isEmpty();
    }
}
