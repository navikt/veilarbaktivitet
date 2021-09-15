package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import no.nav.common.job.leader_election.LeaderElectionClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class SendOppgaveCron {
    private final LeaderElectionClient leaderElectionClient;
    private final BrukerNotifkasjonOppgaveService internalService;

    @Scheduled(initialDelay = 1000, fixedDelay = 1000)
    void sendBrukernotifikasjoner() {
        if (leaderElectionClient.isLeader()) {
            sendAlle(500);
        }
    }

    void sendAlle(int maxBatchSize) {
        while (sendOpptil(maxBatchSize) == maxBatchSize) ;
    }

    private int sendOpptil(int maxAntall) {
        List<SkalSendes> skalSendes = internalService.hentVarselSomSkalSendes(maxAntall);
        skalSendes.forEach(internalService::send);
        return skalSendes.size();
    }
}
