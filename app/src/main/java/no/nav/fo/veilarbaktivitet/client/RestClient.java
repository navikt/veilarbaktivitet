package no.nav.fo.veilarbaktivitet.client;

import no.nav.json.JsonProvider;
import org.glassfish.jersey.client.ClientConfig;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.glassfish.jersey.client.ClientProperties.*;

public class RestClient {

    private final Provider<HttpServletRequest> httpServletRequestProvider;
    private final String basePath;

    public RestClient(Provider<HttpServletRequest> httpServletRequestProvider, String basePath) {
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.basePath = basePath;
    }

    public RestRequest request(String relativePath) {
        Client client = createClient();
        HttpServletRequest httpServletRequest = httpServletRequestProvider.get();
        WebTarget webTarget = client.target(basePath + relativePath);
        return new RestRequest(httpServletRequest, webTarget);
    }

    private Client createClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(new JsonProvider());
        clientConfig.property(FOLLOW_REDIRECTS,false);
        clientConfig.property(CONNECT_TIMEOUT,1000);
        clientConfig.property(READ_TIMEOUT,1000);
        return ClientBuilder.newClient(clientConfig);
    }

    public static RestClient build(Provider<HttpServletRequest> httpServletRequestProvider, String basePath) {
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("mangler basePath");
        }
        if (httpServletRequestProvider == null) {
            throw new IllegalArgumentException("mangler httpServletRequestProvider");
        }

        return new RestClient(httpServletRequestProvider, basePath);
    }

}

