package no.nav.veilarbaktivitet.manuell_status.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManuellStatusV2ClientImpl implements ManuellStatusV2Client {
    private final OkHttpClient veilarboppfolgingHttpClient;
    private final PersonService personService;

    private final AzureAdMachineToMachineTokenClient tokenClient;

    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    @Value("${VEILARBOPPFOLGINGAPI_SCOPE}")
    private String tokenScope;

    public Optional<ManuellStatusV2DTO> get(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);
        String accessToken = tokenClient.createMachineToMachineToken(tokenScope);

        String uri = String.format("%s/v2/manuell/status?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, ManuellStatusV2DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
