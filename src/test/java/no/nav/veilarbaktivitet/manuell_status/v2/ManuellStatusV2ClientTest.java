package no.nav.veilarbaktivitet.manuell_status.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ManuellStatusV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String MANUELL_STATUS_RESPONS = "manuell_status/v2/manuellStatusRespons.json";

    private ManuellStatusV2Client manuellStatusV2Client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        Mockito.when(tokenClient.createMachineToMachineToken(anyString())).thenReturn("mockMachineToMachineToken");

        PersonService personService = mock(PersonService.class);
        when(personService.getFnrForAktorId(AKTORID)).thenReturn(FNR);

        manuellStatusV2Client = new ManuellStatusV2ClientImpl(okHttpClient, personService, tokenClient);
        manuellStatusV2Client.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    public void test_manuell_status_ok_response() {

        stubFor(get(urlMatching("/veilarboppfolging/api/v2/manuell/status\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(MANUELL_STATUS_RESPONS)));
        Optional<ManuellStatusV2DTO> manuellStatusV2Response = manuellStatusV2Client.get(AKTORID);

        assertThat(manuellStatusV2Response).get()
                .hasFieldOrPropertyWithValue("erUnderManuellOppfolging", false)
                .hasFieldOrPropertyWithValue("krrStatus.kanVarsles", false)
                .hasFieldOrPropertyWithValue("krrStatus.erReservert", false);
    }

    @Test
    public void test_manuell_status_kall_feiler() {
        stubFor(get(urlMatching("/veilarboppfolging/api/v2/manuell/status\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> manuellStatusV2Client.get(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}