package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LeaderElectionMetrics {
    private final LeaderElectionClient leaderElectionClient;
    private final AtomicInteger isLeader;

    public LeaderElectionMetrics(LeaderElectionClient leaderElectionClient, MeterRegistry meterRegistry) {
        this.leaderElectionClient = leaderElectionClient;
        isLeader = meterRegistry.gauge("isLeader", new AtomicInteger(1));
    }

    @Scheduled(fixedRate = 1000)
    public void updateIsLeaderMetric() {
        boolean leader = leaderElectionClient.isLeader();
        if (leader) {
            isLeader.set(1);
        } else {
            isLeader.set(0);
        }
    }
}