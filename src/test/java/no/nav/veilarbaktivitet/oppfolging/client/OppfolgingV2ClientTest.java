package no.nav.veilarbaktivitet.oppfolging.client;

import de.mkammerer.wiremock.WireMockExtension;
import no.nav.veilarbaktivitet.oppfolging.siste_periode.GjeldendePeriodeMetrikk;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

class OppfolgingV2ClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String OPPFOLGING_RESPONS = "oppfolging/v2/oppfolgingRespons.json";

    private OppfolgingV2ClientImpl oppfolgingV2Client;

    @RegisterExtension
    WireMockExtension wireMock = new WireMockExtension(8089);

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        PersonService personService = Mockito.mock(PersonService.class);
        GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk = Mockito.mock(GjeldendePeriodeMetrikk.class);
        when(personService.getFnrForAktorId(AKTORID)).thenReturn(FNR);
        oppfolgingV2Client = new OppfolgingV2ClientImpl(okHttpClient, personService, gjeldendePeriodeMetrikk);
        oppfolgingV2Client.setBaseUrl("http://localhost:8089/veilarboppfolging/api");
    }

    @Test
    void test_oppfolging_ok_response() {

        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(OPPFOLGING_RESPONS)));
        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingV2Response = oppfolgingV2Client.fetchUnderoppfolging(AKTORID);

        assertThat(oppfolgingV2Response).get()
                .hasFieldOrPropertyWithValue("erUnderOppfolging", true);
    }

    @Test
    void test_oppfolging_kall_feiler() {
        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> oppfolgingV2Client.fetchUnderoppfolging(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}