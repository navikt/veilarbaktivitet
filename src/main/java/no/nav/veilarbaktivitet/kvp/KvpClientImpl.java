package no.nav.veilarbaktivitet.kvp;

import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Service
@RequiredArgsConstructor
class KvpClientImpl implements KvpClient {

    private final String baseUrl = getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);
    private final OkHttpClient client;

    public Optional<KvpDTO> get(Person.AktorId aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == 204) {
                return Optional.empty();
            }

            return RestUtils.parseJsonResponse(response, KvpDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot" + request.url().toString(), e);
        }

    }
}
