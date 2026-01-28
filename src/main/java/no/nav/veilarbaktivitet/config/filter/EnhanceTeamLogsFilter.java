package no.nav.veilarbaktivitet.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.UserInContext;
import jakarta.servlet.Filter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Profile("!test")
@Service
@RequiredArgsConstructor
public class EnhanceTeamLogsFilter implements Filter {

    private final IAuthService authService;

    private final UserInContext userInContext;

    public static final String TEAMLOGS_ER_INTERN_BRUKER = "TeamLogsFilter.erInternBruker";
    public static final String TEAMLOGS_INNLOGGET_BRUKER_IDENT = "TeamLogsFilter.innloggetBrukerIdent";
    public static final String TEAMLOGS_USER_CONTEXT = "TeamLogsFilter.userContext";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();
        String userContext = userInContext.getFnr().map(Person::get).orElse(null);

        MDC.put(TEAMLOGS_ER_INTERN_BRUKER, erInternBruker);
        MDC.put(TEAMLOGS_INNLOGGET_BRUKER_IDENT, innloggetBrukerIdent);
        MDC.put(TEAMLOGS_USER_CONTEXT, userContext);

        filterChain.doFilter(servletRequest, servletResponse);
    }

}
