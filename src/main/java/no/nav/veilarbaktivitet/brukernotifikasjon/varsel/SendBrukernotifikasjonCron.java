package no.nav.veilarbaktivitet.brukernotifikasjon.varsel;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService;
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
    private final MinsideVarselService brukernotifikasjonService;
    private final VarselDAO varselDao;
    private final VarselMetrikk varselMetrikk;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "send_brukernotifikasjoner", lockAtMostFor = "PT20M")
    public Integer sendBrukernotifikasjoner() {
            return sendAlle(500);
    }

    /* Public fordi den er brukt direkte i noen tester for å hoppe over scheduler lock  */
    public int sendAlle(int maxBatchSize) {
        brukernotifikasjonService.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter();
        var total = 0;
        var currentBatch = 0;
        do {
            currentBatch = sendOpptil(maxBatchSize);
            total += currentBatch;
        } while (currentBatch == maxBatchSize);
        return total;
    }

    private int sendOpptil(int maxAntall) {
        List<SkalSendes> skalSendes = brukernotifikasjonService.hentVarselSomSkalSendes(maxAntall);
        skalSendes.forEach(brukernotifikasjonService::send);
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
