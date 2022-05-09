package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
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

import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeilarbarenaClient {
    private final OkHttpClient veilarbarenaClient;

    private final AzureAdMachineToMachineTokenClient tokenClient;

    private final String cluster = getOptionalProperty("NAIS_CLUSTER_NAME").orElse("");

    private final String tokenScope = String.format("api://%s.%s.%s/.default", cluster, "pto", "veilarbarena");

    @Value("${VEILARBARENAAPI_URL}")
    private String baseUrl;

    public Optional<AktiviteterDTO> hentAktiviteter(Person.Fnr fnr) {
        String accessToken = tokenClient.createMachineToMachineToken(tokenScope);

        String uri = String.format("%s/arena/aktiviteter?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .url(uri)
                .build();
        try (Response response = veilarbarenaClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }

            return RestUtils.parseJsonResponse(response, AktiviteterDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot veilarbarena", e);
        }
    }

}
