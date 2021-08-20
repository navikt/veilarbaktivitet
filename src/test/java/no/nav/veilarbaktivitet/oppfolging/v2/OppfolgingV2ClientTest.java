package no.nav.veilarbaktivitet.oppfolging.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbaktivitet.domain.Person;
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

public class OppfolgingV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String OPPFOLGING_RESPONS ="oppfolging/v2/oppfolgingRespons.json";

    private OppfolgingV2Client oppfolgingV2Client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AuthService authService = Mockito.mock(AuthService.class);
        when(authService.getFnrForAktorId(AKTORID)).thenReturn(Optional.of(FNR));
        oppfolgingV2Client = new OppfolgingV2ClientImpl(okHttpClient, authService);
        oppfolgingV2Client.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    public void test_oppfolging_ok_response() {

        stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(OPPFOLGING_RESPONS)));
        Optional<OppfolgingV2Response> oppfolgingV2Response = oppfolgingV2Client.get(AKTORID);

        assertThat(oppfolgingV2Response).get()
                .hasFieldOrPropertyWithValue("erUnderOppfolging", true);
    }

    @Test
    public void test_oppfolging_kall_feiler() {
        stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> oppfolgingV2Client.get(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}