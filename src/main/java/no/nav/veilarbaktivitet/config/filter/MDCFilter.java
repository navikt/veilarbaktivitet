package no.nav.veilarbaktivitet.config.filter;

import no.nav.veilarbaktivitet.aktivitet.MetricService;
import org.jboss.logging.MDC;

import javax.servlet.*;
import java.io.IOException;

public class MDCFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MDC.put(MetricService.SOURCE, "veilarbaktivitet");
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(MetricService.SOURCE);
        }
    }
}
