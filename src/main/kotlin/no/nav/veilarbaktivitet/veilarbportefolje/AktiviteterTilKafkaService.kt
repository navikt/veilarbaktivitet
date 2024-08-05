package no.nav.veilarbaktivitet.veilarbportefolje

import io.micrometer.core.annotation.Timed
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.job.JobRunner
import no.nav.veilarbaktivitet.util.BatchJob
import no.nav.veilarbaktivitet.util.BatchResult
import no.nav.veilarbaktivitet.util.BatchTrackingDAO
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Slf4j
@RequiredArgsConstructor
class AktiviteterTilKafkaService(
    private val dao: KafkaAktivitetDAO,
    private val producerService: AktivitetKafkaProducerService,
    private val batchTrackingDAO: BatchTrackingDAO
) {


    @Scheduled(
        initialDelayString = "\${app.env.scheduled.portefolje.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.portefolje.fixedDelay}"
    )
    @SchedulerLock(name = "aktiviteter_kafka_scheduledTask", lockAtMostFor = "PT2M")
    @Timed
    fun sendOppTil5000AktiviterTilPortefolje() {
        val maksAntall = 5000L
        JobRunner.run("aktiviteter_til_portefolje_paa_kafka") {
            batchTrackingDAO.withOffset(BatchJob.Aktiviteter_til_portefolje) { sisteProsesserteVersjon ->
                val meldinger = dao.hentAktiviteterSomIkkeErSendtTilPortefoljePaAiven(sisteProsesserteVersjon, maksAntall)
                meldinger.map {
                    try {
                        producerService.sendAktivitetMelding(it)
                        return@map BatchResult.Success(it.version)
                    } catch (e: Exception) {
                        return@map BatchResult.Failure(it.version)
                    }
                }
            }
        }
    }
}