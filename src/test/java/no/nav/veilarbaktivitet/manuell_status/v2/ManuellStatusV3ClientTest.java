package no.nav.veilarbaktivitet.manuell_status.v2;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.OkHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ManuellStatusV3ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String MANUELL_STATUS_RESPONS = "manuell_status/v2/manuellStatusRespons.json";

    private ManuellStatusV2Client manuellStatusV2Client;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @BeforeEach
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        PersonService authService = Mockito.mock(PersonService.class);
        when(authService.getFnrForAktorId(AKTORID)).thenReturn(FNR);
        manuellStatusV2Client = new ManuellStatusV3ClientImpl(okHttpClient, authService);
        manuellStatusV2Client.setBaseUrl(wireMock.baseUrl());
    }

    @Test
    void test_manuell_status_ok_response() {

        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/manuell/status\\?fnr=([0-9]*)"))
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
    void test_manuell_status_kall_feiler() {
        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/manuell/status\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> manuellStatusV2Client.get(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}
