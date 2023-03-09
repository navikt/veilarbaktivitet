package no.nav.veilarbaktivitet.kvp.v2;

import de.mkammerer.wiremock.WireMockExtension;
import no.nav.veilarbaktivitet.person.Person;
import okhttp3.OkHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KvpV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final String KVP_RESPONS = "kvp/v2/kvpRespons.json";

    private KvpV2Client kvpV2Client;

    @RegisterExtension
    WireMockExtension wireMock = new WireMockExtension(0);

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        kvpV2Client = new KvpV2ClientImpl(okHttpClient);
        kvpV2Client.setBaseUrl( wireMock.baseUrl() + "/veilarboppfolging/api");
    }

    @Test
    void test_kvp_ok_response() {

        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/kvp\\?aktorId=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(KVP_RESPONS)));
        Optional<KvpV2DTO> kvpV2DTO = kvpV2Client.get(AKTORID);

        assertThat(kvpV2DTO).get()
                .hasFieldOrPropertyWithValue("enhet", "1234")
                .hasFieldOrPropertyWithValue("avsluttetDato", null);
    }

    @Test
    void test_kvp_kall_feiler() {
        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/kvp\\?aktorId=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> kvpV2Client.get(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }

    @Test
    void test_kvp_httpcode_204() {
        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/kvp\\?aktorId=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "text/json")));
        Optional<KvpV2DTO> kvpV2DTO = kvpV2Client.get(AKTORID);
        assertThat(kvpV2DTO).isEmpty();
    }
}
