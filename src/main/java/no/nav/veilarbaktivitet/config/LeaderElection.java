package no.nav.veilarbaktivitet.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.job.leader_election.ShedLockLeaderElectionClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("!dev")
public class LeaderElection {

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbc) {
        JdbcTemplateLockProvider.Configuration config = JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbc)
                .usingDbTime()
                .build();

        return new JdbcTemplateLockProvider(config);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient(LockProvider lockProvider) {
        return new ShedLockLeaderElectionClient(lockProvider);
    }
}
