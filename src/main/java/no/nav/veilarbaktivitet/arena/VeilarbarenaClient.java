package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.arena.VeilarbarenaHelsesjekk.HealthStatus;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.person.Person;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VeilarbarenaClient {
    private final OkHttpClient veilarbarenaHttpClient;

    @Value("${app.env.veilarena.serviceurl}")
    private String veilarbarenaServiceUrl;

    public HealthStatus  ping() {
        String uri = String.format("%s/veilarbarena/internal/selftest", veilarbarenaServiceUrl);
        // This endpoint this not need auth and therfore uses baseClient
        var basicHttpClient = RestClient.baseClientBuilder().build();
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = basicHttpClient.newCall(request).execute()) {
            if (response.code() != HttpStatus.OK.value()) {
                return HealthStatus.ERROR;
            }
        } catch (Exception e) {
            return HealthStatus.UNAVAILABLE;
        }
        return HealthStatus.OK;
    }

    @Timed
    public Optional<AktiviteterDTO> hentAktiviteter(Person.Fnr fnr) {
        String uri = String.format("%s/veilarbarena/api/arena/aktiviteter?fnr=%s", veilarbarenaServiceUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = veilarbarenaHttpClient.newCall(request).execute()) {
            log.info("Veilarbarena respons: {}", response);
            RestUtils.throwIfNotSuccessful(response);

            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonResponse(response, AktiviteterDTO.class);
        } catch (Exception e) {
            log.error("Feil ved henting av aktiviteter fra veilarbarena", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Feil ved kall mot veilarbarena", e);
        }
    }

}
