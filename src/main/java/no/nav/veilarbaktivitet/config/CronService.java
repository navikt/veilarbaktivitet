package no.nav.veilarbaktivitet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.avtalt_med_nav.varsel.AvtaltVarselService;
import no.nav.veilarbaktivitet.motesms.MoteSMSService;
import no.nav.veilarbaktivitet.veilarbportefolje.AktiviteterTilKafkaService;
import no.nav.veilarbaktivitet.veilarbportefolje.gammel.AktiviteterTilPortefoljeService;
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
    private final AktiviteterTilKafkaService aktiviteterTilKafkaService;
    private final AvtaltVarselService avtaltVarselService;
    private final UnleashClient unleashClient;

    public static final String STOPP_AKTIVITETER_TIL_KAFKA = "veilarbaktivitet.stoppAktiviteterTilKafka";

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
    public void sendMeldingerTilPortefoljeOnprem() {
        if (leaderElectionClient.isLeader() && !unleashClient.isEnabled(STOPP_AKTIVITETER_TIL_KAFKA)) {
            aktiviteterTilPortefoljeService.sendOppTil5000AktiviterTilPortefolje();
        }
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.portefolje.initialDelay}",
            fixedDelayString = "${app.env.scheduled.portefolje.fixedDelay}"
    )
    public void sendMeldingerTilPortefoljeAiven() {
        if (leaderElectionClient.isLeader() && !unleashClient.isEnabled(STOPP_AKTIVITETER_TIL_KAFKA)) {
            aktiviteterTilKafkaService.sendOppTil5000AktiviterTilPortefolje();
        }
    }

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
