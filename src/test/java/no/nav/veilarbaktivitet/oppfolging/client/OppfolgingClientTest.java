package no.nav.veilarbaktivitet.oppfolging.client;

import de.mkammerer.wiremock.WireMockExtension;
import no.nav.veilarbaktivitet.oppfolging.periode.GjeldendePeriodeMetrikk;
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
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class OppfolgingClientTest {
    private static final Person.AktorId AKTORID = Person.aktorId("1234");
    private static final Person.Fnr FNR = Person.fnr("10108000398");
    private static final String OPPFOLGING_RESPONS = "oppfolging/v2/oppfolgingRespons.json";

    private OppfolgingClientImpl oppfolgingClient;


    @RegisterExtension
    WireMockExtension wireMock = new WireMockExtension(0);

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        PersonService personService = Mockito.mock(PersonService.class);
        GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk = Mockito.mock(GjeldendePeriodeMetrikk.class);
        when(personService.getFnrForAktorId(AKTORID)).thenReturn(FNR);
        oppfolgingClient = new OppfolgingClientImpl(okHttpClient, okHttpClient, personService, gjeldendePeriodeMetrikk);
        wireMock.getBaseUri();
        oppfolgingClient.setBaseUrl(wireMock.baseUrl() + "/veilarboppfolging/api");
    }

    @Test
    void test_oppfolging_ok_response() {

        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(OPPFOLGING_RESPONS)));
        Optional<OppfolgingV2UnderOppfolgingDTO> oppfolgingV2Response = oppfolgingClient.fetchUnderoppfolging(AKTORID);

        assertThat(oppfolgingV2Response).get()
                .hasFieldOrPropertyWithValue("erUnderOppfolging", true);
    }

    @Test
    void test_oppfolging_kall_feiler() {
        wireMock.stubFor(get(urlMatching("/veilarboppfolging/api/v2/oppfolging\\?fnr=([0-9]*)"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Exception exception = assertThrows(ResponseStatusException.class, () -> oppfolgingClient.fetchUnderoppfolging(AKTORID));
        MatcherAssert.assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }

    @Test
    void test_hentsak_ok_response() {
        var uuid = UUID.randomUUID();
        var sakId = 1000;
        wireMock.stubFor(post(urlMatching("/veilarboppfolging/api/v3/sak/" + uuid))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody("""
                                {
                                "oppfolgingsperiodeId": "%s",
                                "sakId": %d
                                }
                                """.formatted(uuid, sakId))));
        Optional<SakDTO> sak = oppfolgingClient.hentSak(uuid);

        assertThat(sak).isPresent();
        assertThat(sak.get().oppfolgingsperiodeId()).isEqualTo(uuid);
        assertThat(sak.get().sakId()).isEqualTo(sakId);
    }
}
