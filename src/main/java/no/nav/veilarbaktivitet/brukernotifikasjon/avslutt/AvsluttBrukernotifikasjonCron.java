package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class AvsluttBrukernotifikasjonCron {
    private final AvsluttSender internalService;

    @Scheduled(
            initialDelayString = "${app.env.scheduled.default.initialDelay}",
            fixedDelayString = "${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "avslutt_brukernotifikasjoner", lockAtMostFor = "PT20M")
    public void avsluttBrukernotifikasjoner() {
            sendAvsluttAlle(500);
    }

    void sendAvsluttAlle(int maxBatchSize) {
        internalService.avsluttIkkeSendteOppgaver();
        internalService.markerAvslutteterAktiviteterSomSkalAvsluttes();
        while (sendAvsluttOpptil(maxBatchSize) == maxBatchSize) ;
    }

    private int sendAvsluttOpptil(int maxAntall) {
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
