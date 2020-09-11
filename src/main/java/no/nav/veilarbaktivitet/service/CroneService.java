package no.nav.veilarbaktivitet.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class CroneService {
    private final MoteSMSService moteSMSService;
    private final LeaderElectionClient leaderElectionClient;

    public CroneService(MoteSMSService moteSMSService, LeaderElectionClient leaderElectionClient) {
        this.moteSMSService = moteSMSService;
        this.leaderElectionClient = leaderElectionClient;
    }

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void sendMoteServicemelding() {
        if (leaderElectionClient.isLeader()) {
            MDC.put("running.job", "moteSmsService");
            moteSMSService.sendServicemeldingerForNesteDogn();
            MDC.clear();
        }
    }
}
