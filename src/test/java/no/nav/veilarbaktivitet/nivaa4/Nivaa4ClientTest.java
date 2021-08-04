package no.nav.veilarbaktivitet.nivaa4;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.veilarbaktivitet.config.ApplicationContext;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AuthService;
import okhttp3.OkHttpClient;
import org.junit.*;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class Nivaa4ClientTest {

    private static final String AKTORID = "1234";
    private static final String FNR = "10108000398";
    private static final String HAR_NIVAA4_OK_RESPONS = "nivaa4/nivaa4OkRespons.json";
    Nivaa4Client nivaa4Client;

    @BeforeClass
    public static void init() {
        System.setProperty(ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY,"http://localhost:8089/veilarbperson/api");
    }

    @Before
    public void setup() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AuthService authService = Mockito.mock(AuthService.class);
        Mockito.when(authService.getFnrForAktorId(Person.aktorId(AKTORID))).thenReturn(Optional.of(Person.fnr(FNR)));
        nivaa4Client = new Nivaa4ClientImpl(okHttpClient, authService);
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Test
    public void test_nivaa4_ok_respons() {
        stubFor(get(urlMatching("/veilarbperson/api/([0-9]*)/harNivaa4"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(HAR_NIVAA4_OK_RESPONS)));
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(Person.aktorId(AKTORID));
        assertThat(nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false), is(true));
    }

    @Test
    public void test_nivaa4_httpcode_204() {
        stubFor(get(urlMatching("/veilarbperson/api/([0-9]*)/harNivaa4"))
                .willReturn(aResponse()
                        .withStatus(204)
                        .withHeader("Content-Type", "text/json")));
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(Person.aktorId(AKTORID));
        assertThat(nivaa4DTO, is(Optional.empty()));
    }

    @Test
    public void test_nivaa4_kall_feiler() {
        stubFor(get(urlMatching("/veilarbperson/api/([0-9]*)/harNivaa4"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Person.AktorId aktorId = Person.aktorId(AKTORID);
        Exception exception = assertThrows(ResponseStatusException.class, () -> nivaa4Client.get(aktorId));
        assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}