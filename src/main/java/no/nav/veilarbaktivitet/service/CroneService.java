package no.nav.veilarbaktivitet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.veilarbaktivitet.aktiviterTilKafka.AktiviteterTilKafkaService;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CroneService {
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

    @Scheduled(fixedRate = 1000, initialDelay = 1000)
    public void sendMeldingerPaaKafka() {
        if (leaderElectionClient.isLeader()) {
            aktiviteterTilKafkaService.sendOppTil5000AktiviterPaaKafka();
            aktiviteterTilKafkaService.sendOppTil5000AktiviterPaaKafkaV4();
        }

    }
}
