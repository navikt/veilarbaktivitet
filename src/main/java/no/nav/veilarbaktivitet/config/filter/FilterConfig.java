package no.nav.veilarbaktivitet.config.filter;

import no.nav.common.auth.context.UserRole;
import no.nav.common.auth.oidc.filter.AzureAdUserRoleResolver;
import no.nav.common.auth.oidc.filter.OidcAuthenticationFilter;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.rest.filter.LogRequestFilter;
import no.nav.common.rest.filter.SetStandardHttpHeadersFilter;
import no.nav.common.token_client.utils.env.TokenXEnvironmentvariables;
import no.nav.veilarbaktivitet.config.EnvironmentProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.common.auth.oidc.filter.OidcAuthenticator.fromConfigs;
import static no.nav.common.utils.EnvironmentUtils.isDevelopment;
import static no.nav.common.utils.EnvironmentUtils.requireApplicationName;

@Profile("!test")
@Configuration
public class FilterConfig {

    private OidcAuthenticatorConfig naisAzureAdConfig(EnvironmentProperties properties) {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(properties.getNaisAadDiscoveryUrl())
                .withClientId(properties.getNaisAadClientId())
                .withUserRoleResolver(new AzureAdUserRoleResolver());
    }

    private OidcAuthenticatorConfig tokenxConfig() {
        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(System.getenv(TokenXEnvironmentvariables.TOKEN_X_WELL_KNOWN_URL))
                .withClientId(System.getenv(TokenXEnvironmentvariables.TOKEN_X_CLIENT_ID))
                .withUserRole(UserRole.EKSTERN);
    }

    @Bean
    public FilterRegistrationBean<PingFilter> pingFilter() {
        // Veilarbproxy trenger dette endepunktet for å sjekke at tjenesten lever
        // /internal kan ikke brukes siden det blir stoppet før det kommer frem

        FilterRegistrationBean<PingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PingFilter());
        registration.setOrder(1);
        registration.addUrlPatterns("/api/ping");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LogRequestFilter> logFilterRegistrationBean() {
        FilterRegistrationBean<LogRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LogRequestFilter(requireApplicationName(), isDevelopment().orElse(false)));
        registration.setOrder(2);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TeamLogsFilter> teamLogsFilterRegistrationBean() {
        FilterRegistrationBean<TeamLogsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TeamLogsFilter());
        registration.addUrlPatterns("/api/*");
        registration.addUrlPatterns("/graphql");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OidcAuthenticationFilter> authenticationFilterRegistrationBean(EnvironmentProperties properties) {
        FilterRegistrationBean<OidcAuthenticationFilter> registration = new FilterRegistrationBean<>();
        OidcAuthenticationFilter authenticationFilter = new OidcAuthenticationFilter(
                fromConfigs(
                        naisAzureAdConfig(properties),
                        tokenxConfig()
                )
        );

        registration.setFilter(authenticationFilter);
        registration.setOrder(4);
        registration.addUrlPatterns("/api/*");
        registration.addUrlPatterns("/internal/api/*");
        registration.addUrlPatterns("/graphql");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<EnhanceTeamLogsFilter> enhanceTeamLogsFilterRegistrationBean(EnhanceTeamLogsFilter enhanceTeamLogsFilter) {
        FilterRegistrationBean<EnhanceTeamLogsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(enhanceTeamLogsFilter);
        registration.addUrlPatterns("/api/*");
        registration.addUrlPatterns("/graphql");
        registration.setOrder(5);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SetStandardHttpHeadersFilter> setStandardHeadersFilterRegistrationBean() {
        FilterRegistrationBean<SetStandardHttpHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SetStandardHttpHeadersFilter());
        registration.setOrder(7);
        registration.addUrlPatterns("/*");
        return registration;
    }

}
