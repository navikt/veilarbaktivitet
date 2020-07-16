package no.nav.veilarbaktivitet.client;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class KvpClient {

    private final String baseUrl;
    private final OkHttpClient client;

    public KvpClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = RestClient.baseClient();
    }

    public static String createBearerToken() {
        return SubjectHandler.getSsoToken().map(SsoToken::getToken).map(token -> "Bearer " + token).orElse(null);
    }

    public KvpDTO get(Person.AktorId aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId.get());
        Request request = new Request.Builder()
                .url(uri)
                .header(HttpHeaders.AUTHORIZATION, createBearerToken())
                .build();
        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, KvpDTO.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved kall mot" + request.url().toString(), e);
        }

    }
}
