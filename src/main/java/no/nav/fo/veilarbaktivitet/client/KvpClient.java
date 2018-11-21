package no.nav.fo.veilarbaktivitet.client;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;

public class KvpClient {

    private final String baseUrl;
    private final Client client;

    public KvpClient(String baseUrl, Client client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

    public KvpDTO get(Person.AktorId aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId.get());
        Invocation.Builder b = client.target(uri).request();
        b.header("Authorization", "Bearer " + this.systemUserTokenProvider.getToken());
        return b.get(KvpDTO.class);
    }
}
