package no.nav.veilarbaktivitet.oppfolging.client;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.oppfolging.periode.GjeldendePeriodeMetrikk;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingClientImpl implements OppfolgingClient {
    private final OkHttpClient veilarboppfolgingHttpClient;
    private final OkHttpClient veilarboppfolgingOnBehalfOfHttpClient;
    private final PersonService personService;
    private final GjeldendePeriodeMetrikk gjeldendePeriodeMetrikk;

    @Setter
    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    public Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = "%s/veilarboppfolging/api/v2/oppfolging?fnr=%s".formatted(baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, OppfolgingV2UnderOppfolgingDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Override
    @Timed
    public Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = "%s/veilarboppfolging/api/v2/oppfolging/periode/gjeldende?fnr=%s".formatted(baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                gjeldendePeriodeMetrikk.tellKallTilEksternOppfolgingsperiode(false);
                return Optional.empty();
            }
            gjeldendePeriodeMetrikk.tellKallTilEksternOppfolgingsperiode(true);
            return RestUtils.parseJsonResponse(response, OppfolgingPeriodeMinimalDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Timed
    @Override
    public List<OppfolgingPeriodeMinimalDTO> hentOppfolgingsperioder(Person.AktorId aktorId) {

        String uri = "%s/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=%s".formatted(baseUrl, aktorId.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
           return RestUtils.parseJsonResponseArrayOrThrow(response, OppfolgingPeriodeMinimalDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Override
    public Optional<SakDTO> hentSak(UUID oppfolgingsperiodeId) {
        String uri = "%s/veilarboppfolging/api/v3/sak/%s".formatted(baseUrl, oppfolgingsperiodeId);
        Request request = new Request.Builder()
                .url(uri)
                .post(RequestBody.create("", null))
                .build();
        try (Response response = veilarboppfolgingOnBehalfOfHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonResponse(response, SakDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    @Override
    public Optional<MålDTO> hentMål(Person.Fnr fnr) {
        String uri = "%s/veilarboppfolging/api/oppfolging/mal?fnr=%s".formatted(baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .get()
                .build();
        try (Response response = veilarboppfolgingOnBehalfOfHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            if (response.code() == HttpStatus.NO_CONTENT.value()) {
                return Optional.empty();
            }
            return RestUtils.parseJsonResponse(response, MålDTO.class);
        } catch (Exception e) {
            throw internalServerError(e, request.url().toString());
        }
    }

    private ResponseStatusException internalServerError(Exception cause, String url) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot %s - %s".formatted(url, cause.getMessage()), cause);
    }
}
