package no.nav.veilarbaktivitet.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static no.nav.veilarbaktivitet.config.TeamLog.teamLog;

@Profile("!test")
@Service
@RequiredArgsConstructor
public class TeamLogsFilter implements Filter {

    private final IAuthService authService;
    private final UserInContext userInContext;


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();
        String userContext = userInContext.getFnr().map(Person::get).orElse(null);

        String msg = "status=%s, method=%s, host=%s, path=%s, erInternBruker=%s, innloggetIdent=%s, queryString=%s, userContext=%s".formatted(
                httpResponse.getStatus(),
                httpRequest.getMethod(),
                httpRequest.getServerName(),
                httpRequest.getRequestURI(),
                erInternBruker,
                innloggetBrukerIdent,
                httpRequest.getQueryString(),
                userContext
        );
        teamLog.info(msg);
    }
}
