package no.nav.veilarbaktivitet.oppfolging.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String OPPFOLGING_RESPONS ="oppfolging/v2/oppfolgingRespons.json";

    private OppfolgingV2ClientImpl oppfolgingV2Client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        Mockito.when(tokenClient.createMachineToMachineToken(any())).thenReturn("mockMachineToMachineToken");

        PersonService personService = Mockito.mock(PersonService.class);
        GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk = Mockito.mock(GjeldendePeriodeMetrikk.class);
        when(personService.getFnrForAktorId(AKTORID)).thenReturn(FNR);

        oppfolgingV2Client = new OppfolgingV2ClientImpl(okHttpClient, personService, gjeldendePeriodeMetrikk, tokenClient);
        oppfolgingV2Client.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    public void test_oppfolging_ok_response() {

        stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(OPPFOLGING_RESPONS)));
        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingV2Response = oppfolgingV2Client.fetchUnderoppfolging(AKTORID);

        assertThat(oppfolgingV2Response).get()
                .hasFieldOrPropertyWithValue("erUnderOppfolging", true);
    }

    @Test
    public void test_oppfolging_kall_feiler() {
        stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> oppfolgingV2Client.fetchUnderoppfolging(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}