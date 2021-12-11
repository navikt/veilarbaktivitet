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
    private final OppgaveDao oppgaveDao;
    private final OppgaveMetrikk oppgaveMetrikk;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void sendBrukernotifikasjoner() {
        if (leaderElectionClient.isLeader()) {
            sendAlle(500);
        }
    }

    void sendAlle(int maxBatchSize) {
        internalService.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter();
        while (sendOpptil(maxBatchSize) == maxBatchSize);
    }

    private int sendOpptil(int maxAntall) {
        List<SkalSendes> skalSendes = internalService.hentVarselSomSkalSendes(maxAntall);
        skalSendes.forEach(internalService::send);
        return skalSendes.size();
    }

    @Scheduled(
            initialDelayString = "${app.env.scheduled.brukernotifikasjon.oppgave.initialDelay}",
            fixedDelayString = "${app.env.scheduled.brukernotifikasjon.oppgave.fixedDelay}"
    )
    public void countForsinkedeVarslerSisteDognet() {
        Integer antall = oppgaveDao.hentAntallUkvitterteVarslerForsoktSendt(20);
        oppgaveMetrikk.countForsinkedeVarslerSisteDognet(antall);
    }
}
