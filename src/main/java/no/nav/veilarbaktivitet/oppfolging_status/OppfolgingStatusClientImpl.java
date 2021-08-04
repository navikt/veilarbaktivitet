package no.nav.veilarbaktivitet.oppfolging_status;

import lombok.RequiredArgsConstructor;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AuthService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARBOPPFOLGINGAPI_URL_PROPERTY;

@Profile("!dev")
@Service
@RequiredArgsConstructor
public class OppfolgingStatusClientImpl implements OppfolgingStatusClient {
    private final String baseUrl = getRequiredProperty(VEILARBOPPFOLGINGAPI_URL_PROPERTY);
    private final OkHttpClient client;
    private final AuthService authService;

    public Optional<OppfolgingStatusDTO> get(Person.AktorId aktorId) {
        Person.Fnr fnr = authService.getFnrForAktorId(aktorId).orElseThrow();

        String uri = String.format("%s/oppfolging?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == 204) {
                return Optional.empty();
            }

            return RestUtils.parseJsonResponse(response, OppfolgingStatusDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }

    }
}
