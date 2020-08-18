package no.nav.veilarbaktivitet.config;

import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Collections;

public class TestSubjectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        SsoToken ssoToken = SsoToken.oidcToken("veileder_test_token", Collections.EMPTY_MAP);
        Subject testSubject = new Subject("z123456", IdentType.InternBruker, ssoToken);
        SubjectHandler.withSubject(testSubject, () -> filterChain.doFilter(servletRequest, servletResponse));
    }

}
