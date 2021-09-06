package no.nav.veilarbaktivitet.oppfolging_status;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.oppfolging.v1.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.oppfolging.v1.OppfolgingStatusClientImpl;
import no.nav.veilarbaktivitet.oppfolging.v1.OppfolgingStatusDTO;
import no.nav.veilarbaktivitet.service.AuthService;
import okhttp3.OkHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class OppfolgingStatusClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String OPPFOLGING_STATUS_RESPONS ="oppfolging_status/oppfolgingStatusRespons.json";

    private OppfolgingStatusClient oppfolgingStatusClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AuthService authService = Mockito.mock(AuthService.class);
        when(authService.getFnrForAktorId(AKTORID)).thenReturn(FNR);
        oppfolgingStatusClient = new OppfolgingStatusClientImpl(okHttpClient, authService);
        oppfolgingStatusClient.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    public void test_oppfolging_status_ok_response() {

        stubFor(get(urlMatching("/veilarboppfolging/api/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(OPPFOLGING_STATUS_RESPONS)));
        Optional<OppfolgingStatusDTO> oppfolgingStatusDTO = oppfolgingStatusClient.get(AKTORID);

        assertThat(oppfolgingStatusDTO).get()
                .hasFieldOrPropertyWithValue("reservasjonKRR", true)
                .hasFieldOrPropertyWithValue("manuell", true)
                .hasFieldOrPropertyWithValue("underOppfolging", true);
    }

    @Test
    public void test_oppfolging_status_httpcode_204() {
        stubFor(get(urlMatching("/veilarboppfolging/api/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "text/json")));
        Optional<OppfolgingStatusDTO> oppfolgingStatusDTO = oppfolgingStatusClient.get(AKTORID);
        assertThat(oppfolgingStatusDTO).isEmpty();
    }

    @Test
    public void test_oppfolging_status_kall_feiler() {
        stubFor(get(urlMatching("/veilarboppfolging/api/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> oppfolgingStatusClient.get(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }





}