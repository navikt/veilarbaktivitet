package no.nav.veilarbaktivitet.config.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

import static no.nav.veilarbaktivitet.config.TeamLog.teamLog;
import static no.nav.veilarbaktivitet.config.filter.EnhanceSecureLogsFilter.*;

@Profile("!test")
public class SecureLogsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String msg = "status=%s, method=%s, host=%s, path=%s, erInternBruker=%s, innloggetIdent=%s, queryString=%s, userContext=%s".formatted(
                httpResponse.getStatus(),
                httpRequest.getMethod(),
                httpRequest.getServerName(),
                httpRequest.getRequestURI(),
                MDC.get(SECURELOGS_ER_INTERN_BRUKER),
                MDC.get(SECURELOGS_INNLOGGET_BRUKER_IDENT),
                httpRequest.getQueryString(),
                MDC.get(SECURELOGS_USER_CONTEXT)
        );
        teamLog.info(msg);
    }

    @Override
    public void destroy() {
        MDC.remove(SECURELOGS_ER_INTERN_BRUKER);
        MDC.remove(SECURELOGS_INNLOGGET_BRUKER_IDENT);
        MDC.remove(SECURELOGS_USER_CONTEXT);

        Filter.super.destroy();
    }
}
