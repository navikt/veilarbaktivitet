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
public class EnhanceSecureLogsFilter implements Filter {

    private final IAuthService authService;

    private final UserInContext userInContext;

    public static final String SECURELOGS_ER_INTERN_BRUKER = "SecureLogsFilter.erInternBruker";
    public static final String SECURELOGS_INNLOGGET_BRUKER_IDENT = "SecureLogsFilter.innloggetBrukerIdent";
    public static final String SECURELOGS_USER_CONTEXT = "SecureLogsFilter.userContext";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String erInternBruker = Boolean.toString(authService.erInternBruker());
        String innloggetBrukerIdent = authService.getLoggedInnUser().get();
        String userContext = userInContext.getFnr().map(Person::get).orElse(null);

        MDC.put(SECURELOGS_ER_INTERN_BRUKER, erInternBruker);
        MDC.put(SECURELOGS_INNLOGGET_BRUKER_IDENT, innloggetBrukerIdent);
        MDC.put(SECURELOGS_USER_CONTEXT, userContext);

        filterChain.doFilter(servletRequest, servletResponse);
    }

}
