package no.nav.veilarbaktivitet.brukernotifikasjon.avlsutt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class AvsluttBrukernotifikasjonCron {
    private final LeaderElectionClient leaderElectionClient;
    private final AvsluttSender internalService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void avsluttBrukernotifikasjoner() {
        if (leaderElectionClient.isLeader()) {
            sendAvsluttAlle(500);
        }
    }

    void sendAvsluttAlle(int maxBatchSize) {
        while (sendAvsluttOpptil(maxBatchSize) == maxBatchSize) ;
    }

    private int sendAvsluttOpptil(int maxAntall) {
        internalService.avsluttIkkeSendteOppgaver();
        List<SkalAvluttes> skalSendes = internalService.getOppgaverSomSkalAvbrytes(maxAntall);
        skalSendes.forEach(this::tryAvsluttOppgave);
        return skalSendes.size();
    }

    private void tryAvsluttOppgave(SkalAvluttes skalAvluttes) {
        try {
            internalService.avsluttOppgave(skalAvluttes);
        } catch (Exception e) {
            log.error("Kunne ikke avslutte oppgave", e);
        }
    }
}
