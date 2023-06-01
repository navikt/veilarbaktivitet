
package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.poao.dab.spring_auth.AuthService;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.mock.MetricsClientMock;
import no.nav.veilarbaktivitet.person.PersonService;
import okhttp3.EventListener;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationTestConfig {

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        Mockito.when(systemUserTokenProvider.getSystemUserToken()).thenReturn("mockSystemUserToken");
        return systemUserTokenProvider;
    }

    @Bean
    public AzureAdMachineToMachineTokenClient tokenClient() {
        AzureAdMachineToMachineTokenClient tokenClient = mock(AzureAdMachineToMachineTokenClient.class);
        Mockito.when(tokenClient.createMachineToMachineToken(any())).thenReturn("mockMachineToMachineToken");
        return tokenClient;
    }
    @Bean
    EventListener metricListener() {
        return Mockito.mock(EventListener.class);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public DataSource dataSource() {
        return LocalH2Database.getPresistentDb().getDataSource();
    }

    @Bean
    UnleashClient unleashClient() {
        return mock(UnleashClient.class);
    }

    @Bean
    public String pdlUrl(Environment environment) {
        return environment.getProperty("app.env.pdl-url");
    }
  
    @Bean
    public IAuthService authService(AuthContextHolder authContextHolder, Pep pep, PersonService personService) {
        return new AuthService(authContextHolder, pep, personService);
    }
}