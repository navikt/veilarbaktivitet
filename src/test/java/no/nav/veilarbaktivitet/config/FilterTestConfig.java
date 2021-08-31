package no.nav.veilarbaktivitet.config;

import no.nav.common.auth.context.UserRole;
import no.nav.common.test.auth.TestAuthContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterTestConfig {

    @Bean
    public FilterRegistrationBean testSubjectFilterRegistrationBean() {
        FilterRegistrationBean<TestAuthContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TestAuthContextFilter(UserRole.INTERN, "Z123456"));
        registration.setOrder(1);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
