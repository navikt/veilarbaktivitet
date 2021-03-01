package no.nav.veilarbaktivitet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.aktiviterTilKafka.AktiviteterTilKafkaService;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class CronService {

    private final MoteSMSService moteSMSService;
    private final LeaderElectionClient leaderElectionClient;
    private final AktiviteterTilKafkaService aktiviteterTilKafkaService;

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    public void sendMoteServicemelding() {
        if (leaderElectionClient.isLeader()) {
            MDC.put("running.job", "moteSmsService");
            moteSMSService.sendServicemeldingerForNesteDogn();
            MDC.clear();
        }
    }

    @Scheduled(fixedRate = 500, initialDelay = 5000)
    public void sendMeldingerPaaKafka() {
        if (leaderElectionClient.isLeader()) {
            aktiviteterTilKafkaService.sendOppTil5000AktiviterPaaKafka();
        }

    }
}
