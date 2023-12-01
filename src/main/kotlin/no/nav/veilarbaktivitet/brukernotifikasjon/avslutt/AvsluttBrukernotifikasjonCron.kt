package no.nav.veilarbaktivitet.brukernotifikasjon.avslutt

import jakarta.annotation.PreDestroy
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

@Service
@EnableScheduling
internal class AvsluttBrukernotifikasjonCron(private val internalService: AvsluttSender) {
    private val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "avslutt_brukernotifikasjoner", lockAtMostFor = "PT20M")
    fun avsluttBrukernotifikasjoner() {
        sendAvsluttAlle(500)
    }

    fun sendAvsluttAlle(maxBatchSize: Int) {
        internalService.avsluttIkkeSendteOppgaver()
        internalService.markerAvslutteterAktiviteterSomSkalAvsluttes()
        while (sendAvsluttOpptil(maxBatchSize) == maxBatchSize);
    }

    private fun sendAvsluttOpptil(maxAntall: Int): Int {
        val skalSendes = internalService.getOppgaverSomSkalAvbrytes(maxAntall)
        skalSendes.forEach(Consumer { skalAvluttes: SkalAvluttes -> this.tryAvsluttOppgave(skalAvluttes) })
        return skalSendes.size
    }

    private fun tryAvsluttOppgave(skalAvluttes: SkalAvluttes) {
        try {
            internalService.avsluttOppgave(skalAvluttes)
        } catch (e: Exception) {
            log.error("Kunne ikke avslutte oppgave", e)
        }
    }

    @PreDestroy
    @ExcludeFromCoverageGenerated
    fun stopScheduler() {
        scheduledExecutorService.shutdown()
    }
}
