package no.nav.veilarbaktivitet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.veilarbaktivitet.avtalt_med_nav.varsel.AvtaltVarselService;
import no.nav.veilarbaktivitet.motesms.MoteSMSService;
import no.nav.veilarbaktivitet.motesms.gammel.MoteSMSMqService;
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

    private final MoteSMSMqService moteSMSMqService;
    private final LeaderElectionClient leaderElectionClient;
    private final AktiviteterTilPortefoljeService aktiviteterTilPortefoljeService;
    private final AktiviteterTilKafkaService aktiviteterTilKafkaService;
    private final AvtaltVarselService avtaltVarselService;
    private final UnleashClient unleashClient;
    private final MoteSMSService moteSMSService;

    public static final String STOPP_AKTIVITETER_TIL_KAFKA = "veilarbaktivitet.stoppAktiviteterTilKafka";
    public static final String BRUKERNOTIFIKASJON_MOTE_SMS = "veilarbaktivitet.brukernotifikasjonMoteSms";

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    ) //TODO  flytt denne når vi er ferdig med togle
    public void sendMoteServicemelding() {
        MDC.put("running.job", "moteSmsService");
        if (leaderElectionClient.isLeader()) {
            if (unleashClient.isEnabled(BRUKERNOTIFIKASJON_MOTE_SMS)) {
                moteSMSService.sendMoteSms();
            } else {
                moteSMSMqService.sendServicemeldingerForNesteDogn();
            }
        }
        MDC.clear();
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )//TODO  flytt denne når vi er ferdig med togle BRUKERNOTIFIKASJON_MOTE_SMS
    public void stopMoteServicemelding() {
        MDC.put("running.job", "moteSmsServiceStopper");
        if (leaderElectionClient.isLeader()) {
            moteSMSService.stopMoteSms();
        }
        MDC.clear();
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
