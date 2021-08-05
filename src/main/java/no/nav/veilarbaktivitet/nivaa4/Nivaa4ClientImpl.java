package no.nav.veilarbaktivitet.nivaa4;

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
import static no.nav.veilarbaktivitet.config.ApplicationContext.VEILARBPERSONAPI_URL_PROPERTY;

@Profile("!dev")
@Service
@RequiredArgsConstructor
public class Nivaa4ClientImpl implements Nivaa4Client {
    private final String baseUrl = getRequiredProperty(VEILARBPERSONAPI_URL_PROPERTY);
    private final OkHttpClient client;
    private final AuthService authService;

    @Override
    public Optional<Nivaa4DTO> get(Person.AktorId aktorId) {
        Person.Fnr fnr = authService.getFnrForAktorId(aktorId).orElseThrow();

        String uri = String.format("%s/%s/harNivaa4", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == 204) {
                return Optional.empty();
            }

            return RestUtils.parseJsonResponse(response, Nivaa4DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }
}
