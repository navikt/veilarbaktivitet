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
    private final long connectTimeout;
    private final long readTimeout;

    public RestClient(Provider<HttpServletRequest> httpServletRequestProvider, String basePath, long connectTimeout, long readTimeout) {
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.basePath = basePath;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
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
        clientConfig.property(CONNECT_TIMEOUT, connectTimeout);
        clientConfig.property(READ_TIMEOUT, readTimeout);
        return ClientBuilder.newClient(clientConfig);
    }

    public static RestClient build(Provider<HttpServletRequest> httpServletRequestProvider, String basePath, long connectTimeout, long readTimeout) {
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("mangler basePath");
        }
        if (httpServletRequestProvider == null) {
            throw new IllegalArgumentException("mangler httpServletRequestProvider");
        }

        return new RestClient(httpServletRequestProvider, basePath, connectTimeout, readTimeout);
    }

}

