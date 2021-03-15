package no.nav.veilarbaktivitet.util;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.MapEntriesAppendingMarker;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.UserInContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SecureLogsfilterFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger("SecureLog");

    private final AuthService authService;
    private final UserInContext userInContext;

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        filterChain.doFilter(servletRequest, servletResponse);

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        Map<String, String> map = new HashMap<>();
        userInContext.getFnr().map(it ->map.put("userContext", it.get()));
        authService.getInnloggetBrukerIdent().map(it -> map.put("inloggetIdent", it));
        map.put("status", ""+ response.getStatus());
        map.put("metode", request.getMethod());
        map.put("erinternbruker", ""+ authService.erInternBruker());
        map.put("queryString", request.getQueryString());

        MapEntriesAppendingMarker markers = new MapEntriesAppendingMarker(map);

        log.info(markers, request.getRequestURI());
    }

    @Override
    public void destroy() {
    }

}
