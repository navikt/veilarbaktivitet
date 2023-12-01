package no.nav.veilarbaktivitet.brukernotifikasjon.varsel

import jakarta.annotation.PreDestroy
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

@Service
@EnableScheduling
class SendBrukernotifikasjonCron(
    private val internalService: BrukernotifikasjonProducer,
    private val varselDao: VarselDAO,
    private val varselMetrikk: VarselMetrikk
) {
    private val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)


    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "send_brukernotifikasjoner", lockAtMostFor = "PT20M")
    fun sendBrukernotifikasjoner() {
        sendAlle(500)
    }

    fun sendAlle(maxBatchSize: Int) {
        varselDao.avbrytIkkeSendteOppgaverForAvslutteteAktiviteter()
        while (sendOpptil(maxBatchSize) == maxBatchSize);
    }

    private fun sendOpptil(maxAntall: Int): Int {
        val skalSendes = varselDao.hentVarselSomSkalSendes(maxAntall)
        skalSendes.forEach(internalService::send)
        return skalSendes.size
    }

    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    fun countForsinkedeVarslerSisteDognet() {
        val antall = varselDao.hentAntallUkvitterteVarslerForsoktSendt(20)
        varselMetrikk.countForsinkedeVarslerSisteDognet(antall)
    }

    @PreDestroy
    @ExcludeFromCoverageGenerated
    fun stopScheduler() {
        scheduledExecutorService.shutdown()
    }
}
