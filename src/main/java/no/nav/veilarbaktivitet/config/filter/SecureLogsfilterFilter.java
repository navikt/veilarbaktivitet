package no.nav.veilarbaktivitet.config.filter;

import lombok.RequiredArgsConstructor;
import no.nav.common.log.MarkerBuilder;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Profile("!dev")
@Service
@RequiredArgsConstructor
public class SecureLogsfilterFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger("SecureLog");

    private final AuthService authService;
    private final UserInContext userInContext;


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
                .field("erInternBruker", ""+ authService.erInternBruker())
                .field("innloggetIdent", innloggetBrukerIdent.orElse(null))
                .field("queryString", httpRequest.getQueryString())
                .field("userContext", userInContext.getFnr().map(Person::get).orElse(null))
                .log(log::info);

    }

}
