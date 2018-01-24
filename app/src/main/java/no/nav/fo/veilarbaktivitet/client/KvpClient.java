package no.nav.fo.veilarbaktivitet.client;

import no.nav.fo.veilarboppfolging.rest.domain.KvpDTO;

public class KvpClient {

    private RestClient restClient;

    public KvpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public KvpDTO get(String aktorId) {
        String uri = String.format("/%s/currentStatus", aktorId);
        RestRequest request = restClient.request(uri);
        return request.get(KvpDTO.class);
    }
}
