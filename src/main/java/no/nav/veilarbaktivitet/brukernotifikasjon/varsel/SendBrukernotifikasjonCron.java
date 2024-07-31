package no.nav.veilarbaktivitet.brukernotifikasjon.varsel;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class SendBrukernotifikasjonCron {
    private final BrukerNotifkasjonProducerService internalService;
    private final VarselDAO varselDao;
    private final VarselMetrikk varselMetrikk;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "send_brukernotifikasjoner", lockAtMostFor = "PT20M")
    public void sendBrukernotifikasjoner() {
            sendAlle(500);
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
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    public void countForsinkedeVarslerSisteDognet() {
        int antall = varselDao.hentAntallUkvitterteVarslerForsoktSendt(20);
        varselMetrikk.countForsinkedeVarslerSisteDognet(antall);
    }
    @PreDestroy
    @ExcludeFromCoverageGenerated
    public void stopScheduler() {
        scheduledExecutorService.shutdown();
    }
}
