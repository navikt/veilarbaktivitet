package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class OppfolgingsperiodeCron {
    private final OppfolgingsperiodeService oppfolgingsperiodeServiceAdder;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.oppfolgingsperiode.fixedDelay}"
    )
    @SchedulerLock(name = "addOppfolgingsperioder_scheduledTask",
            lockAtLeastForString = "PT1S", lockAtMostForString = "PT14M")
    public void addOppfolgingsperioder() {
        long antall = oppfolgingsperiodeServiceAdder.oppdater500brukere();
        log.info("oppdatert {} brukere med oppfolginsperiode", antall);
    }
}
