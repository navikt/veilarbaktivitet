package no.nav.veilarbaktivitet.oppfolging.client;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.oppfolging.periode.GjeldendePeriodeMetrikk;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.*;
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

    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    public Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = String.format("%s/v2/oppfolging?fnr=%s", baseUrl, fnr.get());
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

        String uri = String.format("%s/v2/oppfolging/periode/gjeldende?fnr=%s", baseUrl, fnr.get());
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

        String uri = String.format("%s/v2/oppfolging/perioder?aktorId=%s", baseUrl, aktorId.get());
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
        String uri = String.format("%s/v3/sak/%s", baseUrl, oppfolgingsperiodeId );
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
        String uri = String.format("%s/oppfolging/mal?fnr=%s", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .get()
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
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
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Feil ved kall mot %s - %s", url, cause.getMessage()), cause);
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
