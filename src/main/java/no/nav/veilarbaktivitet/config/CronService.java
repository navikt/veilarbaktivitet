package no.nav.veilarbaktivitet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.avtalt_med_nav.varsel.AvtaltVarselService;
import no.nav.veilarbaktivitet.motesms.MoteSMSService;
import no.nav.veilarbaktivitet.veilarbportefolje.AktiviteterTilPortefoljeService;
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
    private final AktiviteterTilPortefoljeService aktiviteterTilPortefoljeService;
    private final AvtaltVarselService avtaltVarselService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void sendMoteServicemelding() {
        if (leaderElectionClient.isLeader()) {
            MDC.put("running.job", "moteSmsService");
            moteSMSService.sendServicemeldingerForNesteDogn();
            MDC.clear();
        }
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.portefolje.initialDelay}",
            fixedDelayString = "${app.env.scheduled.portefolje.fixedDelay}"
    )
    public void sendMeldingerTilPortefolje() {
        if (leaderElectionClient.isLeader()) {
            aktiviteterTilPortefoljeService.sendOppTil5000AktiviterTilPortefolje();
        }
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void handleVarsler() {
        if(leaderElectionClient.isLeader()) {
            avtaltVarselService.stoppVarsel();
            avtaltVarselService.sendVarsel();
        }
    }


}
