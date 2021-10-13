package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelingAvCvCronService {
    private final LeaderElectionClient leaderElectionClient;
    private final DelingAvCvFristUtloptService delingAvCvFristUtloptService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    void avsluttUtlopedeAktiviteter() {
        if (leaderElectionClient.isLeader()) {
            while (delingAvCvFristUtloptService.avsluttUtlopedeAktiviteter(500) == 500) ;
        }
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    void notifiserAvbruttEllerFullfortUtenSvar() {
        if (leaderElectionClient.isLeader()) {
            while (delingAvCvFristUtloptService.notifiserFullfortEllerAvbruttUtenSvar(500) == 500) ;
        }
    }
}
