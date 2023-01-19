package no.nav.veilarbaktivitet.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import org.springframework.stereotype.Service;

import javax.servlet.FilterConfig;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import static no.nav.common.test.auth.AuthTestUtils.TEST_AUDIENCE;
import static no.nav.common.test.auth.AuthTestUtils.TEST_ISSUER;

@Service
public class TestAuthContextFilter implements Filter {
    public static final String identHeder = "test_ident";
    public static final String typeHeder = "test_ident_type";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String test_ident = httpRequest.getHeader(identHeder);
        String test_ident_type = httpRequest.getHeader(typeHeder);

        AuthContext authContext = new AuthContext(
                UserRole.valueOf(test_ident_type),
                new PlainJWT(new JWTClaimsSet.Builder()
                        .subject(test_ident)
                        .audience(TEST_AUDIENCE)
                        .issuer(TEST_ISSUER)
                        .claim("acr", "Level4")
                        .build())
        );

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
