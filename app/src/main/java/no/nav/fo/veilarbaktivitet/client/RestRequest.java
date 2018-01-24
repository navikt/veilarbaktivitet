package no.nav.fo.veilarbaktivitet.client;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import static javax.ws.rs.core.HttpHeaders.COOKIE;

public class RestRequest {

    private final HttpServletRequest httpServletRequest;
    private WebTarget webTarget;

    public RestRequest(HttpServletRequest httpServletRequest, WebTarget webTarget) {
        this.httpServletRequest = httpServletRequest;
        this.webTarget = webTarget;
    }

    public RestRequest queryParam(String name, Object value) {
        webTarget = webTarget.queryParam(name, value);
        return this;
    }

    public <ELEMENT> ELEMENT get(Class<ELEMENT> responseClass) {
        Invocation.Builder request = webTarget.request();
        request.header(COOKIE, httpServletRequest.getHeader(COOKIE));
        return request.get(responseClass);
    }
}

