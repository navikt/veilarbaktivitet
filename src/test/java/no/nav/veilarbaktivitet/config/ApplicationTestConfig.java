
package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.mock.AktorOppslackMock;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.mock.MetricsClientMock;
import no.nav.veilarbaktivitet.mock.PepMock;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import okhttp3.OkHttpClient;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationTestConfig {
    @Bean
    /**
     * OkHttpClient uten SystemUserOidcTokenProviderInterceptor. Se  {@link no.nav.veilarbaktivitet.config.ClientConfig}
     */
    public OkHttpClient client() {
        return new OkHttpClient();
    }

    @Bean
    public KvpClient kvpClient() {
        return mock(KvpClient.class);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        Mockito.when(systemUserTokenProvider.getSystemUserToken()).thenReturn("mockSystemUserToken");
        return systemUserTokenProvider;
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
    public KafkaProducerClient<String, String> kafkaProducerClient() {

        //TODO fiks metode returner
        KafkaProducerClient mock = mock(KafkaProducerClient.class);
        when(mock.sendSync(any())).thenReturn(new RecordMetadata(null, 0, 0, 0, 0L, 1, 1));
        return mock;
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public JmsTemplate varselQueue() {
        return mock(JmsTemplate.class);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return () -> true;
    }

    @Bean
    public DataSource dataSource() {
        return LocalH2Database.getPresistentDb().getDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return LocalH2Database.getPresistentDb();
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock(null);
    }

}

