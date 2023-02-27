package no.nav.veilarbaktivitet.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import no.nav.common.auth.context.AuthContext;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.auth.context.UserRole;
import org.springframework.stereotype.Service;

import static no.nav.common.auth.Constants.AAD_NAV_IDENT_CLAIM;


@Service
public class TestAuthContextFilter implements Filter {

    public final static String TEST_AUDIENCE = "test-audience";

    public final static String TEST_ISSUER = "https://testIssuer.test";

    public static final String identHeder = "test_ident";
    public static final String typeHeder = "test_ident_type";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String test_ident = httpRequest.getHeader(identHeder);
        String test_ident_type = httpRequest.getHeader(typeHeder);

        AuthContext authContext = new AuthContext(
                UserRole.valueOf(test_ident_type),
                new PlainJWT((test_ident_type.equals("EKSTERN") ? brukerClaims(test_ident) : veilederClaims(test_ident)))
        );

        AuthContextHolderThreadLocal.instance().withContext(authContext, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

    private JWTClaimsSet veilederClaims(String test_ident) {
        return new JWTClaimsSet.Builder()
                .subject(test_ident)
                .audience(TEST_AUDIENCE)
                .issuer(TEST_ISSUER)
                .claim(AAD_NAV_IDENT_CLAIM, test_ident)
                .build();
    }
    private JWTClaimsSet brukerClaims(String test_ident) {
        return new JWTClaimsSet.Builder()
                .subject(test_ident)
                .claim("pid", test_ident)
                .claim("acr", "Level4")
                .audience(TEST_AUDIENCE)
                .issuer(TEST_ISSUER)
                .build();
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
