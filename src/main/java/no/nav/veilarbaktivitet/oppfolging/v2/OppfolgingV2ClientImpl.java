package no.nav.veilarbaktivitet.oppfolging.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
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
public class OppfolgingV2ClientImpl implements OppfolgingV2Client {
    private final OkHttpClient client;
    private final PersonService personService;

    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    public Optional<OppfolgingV2UnderOppfolgingDTO> getUnderoppfolging(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = String.format("%s/v2/oppfolging?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, OppfolgingV2UnderOppfolgingDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

    @Override
    public Optional<OppfolgingPeriodeMinimalDTO> getGjeldendePeriode(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = String.format("%s/v2/oppfolging/periode/gjeldende?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonResponse(response, OppfolgingPeriodeMinimalDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot " + request.url(), e);
        }
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
