package no.nav.veilarbaktivitet.config;

import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.TestAuthContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterTestConfig {

    public static final String NAV_IDENT_ITEST = "Z123456";

    @Bean
    public FilterRegistrationBean testSubjectFilterRegistrationBean() {
        FilterRegistrationBean<TestAuthContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TestAuthContextFilter(UserRole.INTERN, NAV_IDENT_ITEST));
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
