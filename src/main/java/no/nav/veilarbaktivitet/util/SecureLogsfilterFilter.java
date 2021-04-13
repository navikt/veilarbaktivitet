package no.nav.veilarbaktivitet.util;

import lombok.RequiredArgsConstructor;
import no.nav.common.log.MarkerBuilder;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.UserInContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

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

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        Optional<String> innloggetBrukerIdent = authService.getInnloggetBrukerIdent();

        new MarkerBuilder()
                .field("status", httpResponse.getStatus())
                .field("method", httpRequest.getMethod())
                .field("host", httpRequest.getServerName())
                .field("path", httpRequest.getRequestURI())
                .field("erinternbruker", ""+ authService.erInternBruker())
                .field("inloggetIdent", innloggetBrukerIdent.orElse(null))
                .field("queryString", httpRequest.getQueryString())
                .field("userContext", userInContext.getFnr().map(Person::get).orElse(null))
                .log(log::info);

    }

    @Override
    public void destroy() {
    }

}
