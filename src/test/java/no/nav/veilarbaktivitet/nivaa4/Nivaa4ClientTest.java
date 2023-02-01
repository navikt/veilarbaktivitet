package no.nav.veilarbaktivitet.nivaa4;

import de.mkammerer.wiremock.WireMockExtension;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class Nivaa4ClientTest {

    private static final String AKTORID = "1234";
    private static final String FNR = "10108000398";
    private static final String HAR_NIVAA4_OK_RESPONS = "nivaa4/nivaa4OkRespons.json";
    Nivaa4Client nivaa4Client;

    @BeforeEach
    void setup() {
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        Mockito.when(tokenClient.createMachineToMachineToken(any())).thenReturn("mockMachineToMachineToken");
        PersonService personService = mock(PersonService.class);
        Mockito.when(personService.getFnrForAktorId(Person.aktorId(AKTORID))).thenReturn(Person.fnr(FNR));
        nivaa4Client = new Nivaa4ClientImpl(wireMock.baseUrl() + "/veilarbperson/api", personService, new OkHttpClient());
    }

    @RegisterExtension
    WireMockExtension wireMock = new WireMockExtension(0);

    @Test
    void test_nivaa4_ok_respons() {
        wireMock.stubFor(get(urlMatching("/veilarbperson/api/person/([0-9]*)/harNivaa4"))
                .willReturn(ok()
                        .withHeader("Content-Type", "text/json")
                        .withBodyFile(HAR_NIVAA4_OK_RESPONS)));
        Optional<Nivaa4DTO> nivaa4DTO = nivaa4Client.get(Person.aktorId(AKTORID));
        assertThat(nivaa4DTO.map(Nivaa4DTO::isHarbruktnivaa4).orElse(false), is(true));
    }

    // TODO lag case for feilhåndtering i itest
    @Test
    void test_nivaa4_kall_feiler() {
        wireMock.stubFor(get(urlMatching("/veilarbperson/api/person/([0-9]*)/harNivaa4"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "text/json")));
        Person.AktorId aktorId = Person.aktorId(AKTORID);
        Exception exception = assertThrows(ResponseStatusException.class, () -> nivaa4Client.get(aktorId));
        assertThat(exception.getMessage(), containsString("Uventet status 400"));
    }
}
