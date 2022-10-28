package no.nav.veilarbaktivitet.config.filter;

import lombok.RequiredArgsConstructor;
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

import static java.lang.String.format;

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

        String msg = format("status=%s, method=%s, host=%s, path=%s, erInternBruker=%b, innloggetIdent=%s, queryString=%s, userContext=%s",
            httpResponse.getStatus(),
            httpRequest.getMethod(),
            httpRequest.getServerName(),
            httpRequest.getRequestURI(),
            authService.erEksternBruker(),
            innloggetBrukerIdent.orElse(null),
            httpRequest.getQueryString(),
            userInContext.getFnr().map(Person::get).orElse(null)
        );
        log.info(msg);
    }

}
