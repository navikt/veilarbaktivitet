package no.nav.veilarbaktivitet.nivaa4;

import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class Nivaa4ClientImpl implements Nivaa4Client {
    private final OkHttpClient veilarbpersonHttpClient;
    private final PersonService personService;
    private final String baseUrl;

    @Autowired
    public Nivaa4ClientImpl(@Value("${VEILARBPERSONAPI_URL}") String baseUrl, PersonService personService, OkHttpClient veilarbpersonHttpClient) {
        this.baseUrl = baseUrl;
        this.personService = personService;
        this.veilarbpersonHttpClient = veilarbpersonHttpClient;
    }


    @Override
    public Optional<Nivaa4DTO> get(Person.AktorId aktorId) {
        Person.Fnr fnr = personService.getFnrForAktorId(aktorId);
        String uri = String.format("%s/person/%s/harNivaa4", baseUrl, fnr.get());
        Request request = new Request.Builder()
                .url(uri)
                .build();

        try (Response response = veilarbpersonHttpClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, Nivaa4DTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("Feil ved kall mot %s - %s", request.url(), e.getMessage()), e);
        }
    }

}
