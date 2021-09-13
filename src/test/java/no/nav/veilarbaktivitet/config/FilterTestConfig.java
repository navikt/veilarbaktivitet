package no.nav.veilarbaktivitet.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterTestConfig {

    public static final String NAV_IDENT_ITEST = "Z123456";

    @Bean
    public FilterRegistrationBean testSubjectFilterRegistrationBean() {
        FilterRegistrationBean<TestAuthContextFilterTingi> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TestAuthContextFilterTingi());
        registration.setOrder(1);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

}
