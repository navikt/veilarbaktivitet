package no.nav.veilarbaktivitet.config;

import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.AuthTestUtils;
import org.springframework.stereotype.Service;

import javax.servlet.FilterConfig;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

@Service
public class TestAuthContextFilterTingi implements Filter {
    public static final String identHeder = "test_ident";
    public static final String typeHeder = "test_ident_type";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String test_ident = httpRequest.getHeader(identHeder);
        String test_ident_type = httpRequest.getHeader(typeHeder);

        AuthContext authContext = AuthTestUtils.createAuthContext(UserRole.valueOf(test_ident_type), test_ident);

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
