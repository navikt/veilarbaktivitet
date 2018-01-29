package no.nav.fo.veilarbaktivitet.client;

import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

import javax.ws.rs.client.Client;

public class KvpClient {

    private final String baseUrl;
    private final Client client;

    public KvpClient(String baseUrl, Client client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    public KvpDTO get(String aktorId) {
        String uri = String.format("%s/kvp/%s/currentStatus", baseUrl, aktorId);
        return client.target(uri).request().get(KvpDTO.class);
    }
}
