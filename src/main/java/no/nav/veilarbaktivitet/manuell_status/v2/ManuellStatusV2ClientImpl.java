package no.nav.veilarbaktivitet.manuell_status.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
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

    @Setter
    @Value("${VEILARBOPPFOLGINGAPI_URL}")
    private String baseUrl;

    public Optional<ManuellStatusV2DTO> get(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);

        String uri = "%s/veilarboppfolging/api/v3/hent-status".formatted(baseUrl);
        Request request = new Request.Builder()
                .post(
                    RequestBody.create(
                        JsonUtils.toJson(new ManuellStatusQueryDto(fnr.get())),
                        MediaType.parse("application/json")
                    )
                )
                .url(uri)
                .build();
        try (Response response = veilarboppfolgingHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, ManuellStatusV2DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot %s - %s".formatted(request.url(), e.getMessage()), e);
        }
    }

    @Data
    @AllArgsConstructor
    class ManuellStatusQueryDto {
        String fnr;
    }
}