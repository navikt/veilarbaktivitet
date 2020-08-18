package no.nav.veilarbaktivitet.config;


import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.veilarbaktivitet.controller.AktivitetsplanController;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.kafka.KafkaService;
import no.nav.veilarbaktivitet.mock.*;
import no.nav.veilarbaktivitet.service.*;
import no.nav.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
@Import({
        Database.class,
        ClientTestConfig.class,
        AktivitetDAO.class,
        BrukerService.class,
        FunksjonelleMetrikker.class,
        AuthService.class,
        AktivitetService.class,
        ArenaAktivitetConsumer.class,
        AktivitetAppService.class,
        AktivitetsplanController.class,
        FilterTestConfig.class,
})
public class ApplicationTestConfig {

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public KafkaService kafkaService() {
        return new KafkaServiceMock();
    }

    @Bean
    public AktorregisterClient aktorregisterClient() {
        return new AktorregisterClientMock();
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        var client = mock(LeaderElectionClient.class);
        when(client.isLeader()).thenAnswer(a -> true);
        return client;
    }

    @Bean
    public DataSource dataSource() {
        return LocalH2Database.getDb().getDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return LocalH2Database.getDb();
    }

    @Bean
    public TiltakOgAktivitetV1 tiltakOgAktivitetV1Client() {
        return new TiltakOgAktivitetMock();
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock(null);
    }
}
