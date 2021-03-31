package no.nav.veilarbaktivitet.config;


import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Credentials;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.veilarbaktivitet.controller.AktivitetsplanController;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.db.dao.KafkaAktivitetDAO;
import no.nav.veilarbaktivitet.db.dao.MoteSmsDAO;
import no.nav.veilarbaktivitet.mock.AktorOppslackMock;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.mock.MetricsClientMock;
import no.nav.veilarbaktivitet.mock.PepMock;
import no.nav.veilarbaktivitet.service.*;
import no.nav.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Configuration
@Import({
        Database.class,
        ClientTestConfig.class,
        AktivitetDAO.class,
        KafkaAktivitetDAO.class,
        MoteSmsDAO.class,
        VarselQueService.class,
        AktiviteterTilKafkaService.class,
        MetricService.class,
        MoteSMSService.class,
        AuthService.class,
        AktivitetService.class,
        TimedConfiguration.class,
        ArenaAktivitetConsumer.class,
        AktivitetAppService.class,
        AktivitetsplanController.class,
        FilterTestConfig.class,
        CronService.class,
})
public class ApplicationTestConfig {

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }

    @Bean
    public KafkaProducerService kafkaProducerService() {
        KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
        when(kafkaProducerService.sendMelding(any())).thenReturn(0L);
        return kafkaProducerService;
    }

    @Bean
    public AktorOppslagClient aktorOppslagClient() {
        return new AktorOppslackMock();
    }

    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public JmsTemplate varselQueue() { return mock(JmsTemplate.class); }

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
    public TiltakOgAktivitetV1 tiltakOgAktivitetV1Client() {
        return new TiltakOgAktivitetMock();
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock(null);
    }
}
