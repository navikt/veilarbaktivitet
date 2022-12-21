package no.nav.veilarbaktivitet.kvp.v2;

import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.veilarbaktivitet.person.Person;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.veilarbaktivitet.util.EnvironmentUtils.isDev;
import static no.nav.veilarbaktivitet.util.EnvironmentUtils.scope;

@Service
@RequiredArgsConstructor
class KvpV2ClientImpl implements KvpV2Client {
    private final AzureAdMachineToMachineTokenClient tokenClient;

    private final OkHttpClient veilarboppfolgingHttpClient;

    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    private final String tokenScope = scope("veilarboppfolging", "pto", isDev() ? "dev-fss" : "prod-fss");

    public Optional<KvpV2DTO> get(Person.AktorId aktorId) {
        String accessToken = tokenClient.createMachineToMachineToken(tokenScope);

        String uri = String.format("%s/v2/kvp?aktorId=%s", baseUrl, aktorId.get());
        Request request = new Request.Builder()
                .url(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == 204) {
                return Optional.empty();
            }

            return RestUtils.parseJsonResponse(response, KvpV2DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot" + request.url(), e);
        }

    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
