package no.nav.veilarbaktivitet.kvp;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.TestData;
import okhttp3.OkHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThrows;

public class KvpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    private KvpClient kvpClient;

    private static String KVP_RESPONS = "kvp/get-oppfolging_current_status-response.json";
    private static Person.AktorId aktorid = TestData.KJENT_AKTOR_ID;

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        kvpClient = new KvpClientImpl(okHttpClient);
        kvpClient.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    public void ok_repons() {
        stubFor(get(urlMatching("/veilarboppfolging/api/kvp/([0-9]*)/currentStatus"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(KVP_RESPONS)));

        Optional<KvpDTO> kvpDTO = kvpClient.get(aktorid);
        assertThat(kvpDTO).get()
                .hasFieldOrPropertyWithValue("aktorId", "4321")
                .hasFieldOrPropertyWithValue("enhet", null)
                .hasFieldOrPropertyWithValue("kvpId", 999L);
    }

    @Test
    public void respons_code_204() {
        stubFor(get(urlMatching("/veilarboppfolging/api/kvp/([0-9]*)/currentStatus"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "text/json")));
        Optional<KvpDTO> kvpDTO = kvpClient.get(aktorid);
        assertThat(kvpDTO).isEmpty();
    }

    @Test
    public void error_fra_tjeneste() {
        stubFor(get(urlMatching("/veilarboppfolging/api/kvp/([0-9]*)/currentStatus"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> kvpClient.get(aktorid));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }

    @Test
    public void ugyldig_json() {
        stubFor(get(urlMatching("/veilarboppfolging/api/kvp/([0-9]*)/currentStatus"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("ugyldig respons")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> kvpClient.get(aktorid));
    }

    @Test
    // Dette er en svakhet ved vår implementasjon, at vi gjør en 'best effort' på deserialiering, men validerer ikke at json er korrekt
    public void ugyldig_respons() {
        stubFor(get(urlMatching("/veilarboppfolging/api/kvp/([0-9]*)/currentStatus"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("{\"ugyldig\": \"json\"}")));
        Optional<KvpDTO> kvpDTO = kvpClient.get(aktorid);
        assertThat(kvpDTO).get()
                .hasAllNullFieldsOrPropertiesExcept("kvpId", "serial"); // long verdier defaulter til 0L
    }



}