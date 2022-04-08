package no.nav.veilarbaktivitet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.avtalt_med_nav.varsel.AvtaltVarselService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class CronService {

    private final LeaderElectionClient leaderElectionClient;
    private final AvtaltVarselService avtaltVarselService;

    public static final String STOPP_AKTIVITETER_TIL_KAFKA = "veilarbaktivitet.stoppAktiviteterTilKafka";


    //TODO slett denen når vi kan rydde bort mq
    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void handleVarsler() {
        if (leaderElectionClient.isLeader()) {
            avtaltVarselService.stoppVarsel();
            avtaltVarselService.sendVarsel();
        }
    }
}
