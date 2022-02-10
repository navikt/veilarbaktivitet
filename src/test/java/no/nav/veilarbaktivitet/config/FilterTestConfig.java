package no.nav.veilarbaktivitet.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterTestConfig {

    @Bean
    public FilterRegistrationBean testSubjectFilterRegistrationBean() {
        FilterRegistrationBean<TestAuthContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TestAuthContextFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/api/*");
        registration.addUrlPatterns("/internal/api/*");
        return registration;
    }

}
