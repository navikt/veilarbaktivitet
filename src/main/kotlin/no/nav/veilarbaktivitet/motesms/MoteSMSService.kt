package no.nav.veilarbaktivitet.motesms

import io.micrometer.core.annotation.Timed
import jakarta.annotation.PreDestroy
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.motesms.MoteSmsDAO.AktivitetVersjon
import no.nav.veilarbaktivitet.util.BatchJob
import no.nav.veilarbaktivitet.util.BatchResult
import no.nav.veilarbaktivitet.util.BatchTrackingDAO
import no.nav.veilarbaktivitet.util.ExcludeFromCoverageGenerated
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

@Service
@Slf4j
@RequiredArgsConstructor
open class MoteSMSService(
    private val moteSmsDAO: MoteSmsDAO,
    private val brukernotifikasjonService: MinsideVarselService,
    private val batchTrackingDAO: BatchTrackingDAO
) {
    private val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "send_mote_sms_scheduledTask", lockAtMostFor = "PT20M")
    @Timed(value = "moteservicemelding", histogram = true)
    open fun sendMoteSms() {
        MDC.put("running.job", "moteSmsService")
        sendServicemeldinger(Duration.ofHours(1), Duration.ofHours(24))
        MDC.clear()
    }

    open fun sendServicemeldinger(fra: Duration, til: Duration) {
        moteSmsDAO.hentMoterUtenVarsel(fra, til, 5000)
            .forEach(Consumer { it: MoteNotifikasjon ->
                if (brukernotifikasjonService.kanVarsles(it.aktorId)) {
                    val varsel = AktivitetVarsel(
                        it.aktivitetId,
                        it.aktitetVersion,
                        it.aktorId,
                        it.getDitNavTekst(),
                        VarselType.MOTE_SMS,
                        it.getEpostTitel(),
                        it.getEpostBody(),
                        it.getSmsTekst()
                    )
                    val varselId = brukernotifikasjonService.opprettVarselPaaAktivitet(varsel)
                    moteSmsDAO.insertGjeldendeSms(it, varselId)
                } else {
                    // Usikker på hvorfor man inserter i gjeldende sms når bruker ikke kan
                    // varsles men det var gjort slik tidligere
                    moteSmsDAO.insertGjeldendeSms(it, null)
                    log.info(
                        "Minside varsel ikke sendt (møte sms), bruker kan ikke varsles {}",
                        it.aktorId
                    )
                }
            })
    }

    @Scheduled(
        initialDelayString = "\${app.env.scheduled.default.initialDelay}",
        fixedDelayString = "\${app.env.scheduled.default.fixedDelay}"
    )
    @SchedulerLock(name = "stopp_mote_sms_scheduledTask", lockAtMostFor = "PT20M")
    @Timed(value = "stopmoteservicemelding", histogram = true)
    open fun stopMoteSms() {
        MDC.put("running.job", "moteSmsServiceStopper")
        stopMoteSms(5000)
        MDC.clear()
    }

    open fun stopMoteSms(maxAntall: Int) {
        val batchResult = batchTrackingDAO.withOffset(BatchJob.StoppMoteSms) {
                sisteProsesserteVersjonFørBatch ->
            moteSmsDAO.hentMoterMedOppdatertTidEllerKanal(maxAntall, sisteProsesserteVersjonFørBatch)
                .map { aktivitetVersjon: AktivitetVersjon ->

                    try {
                        brukernotifikasjonService.setDone(aktivitetVersjon.aktivitetId, VarselType.MOTE_SMS)
                        moteSmsDAO.slettGjeldende(aktivitetVersjon.aktivitetId) //TODO endre til send beskjed sms om flyttet møte + skal sende på nytt hvis møtet er mere enn 48 timer fremm i tid
                        return@map BatchResult.Success(aktivitetVersjon.versjon)
                    } catch (e: Exception) {
                        log.warn("Feil under prosessering av StoppMøteSms batch", e)
                        return@map BatchResult.Failure(aktivitetVersjon.versjon)
                    }
                }
        }
        if (batchResult.size > 0) log.info("Behandlet stoppMøteSms for {} møter som har oppdatert tid eller kanal", batchResult.size)
        moteSmsDAO.hentMoteSmsSomFantStedForMerEnd(Duration.ofDays(7)) //TODO Trenger vi denne? Holder det at bruker kan fjerne den og den forsvinner når aktiviteter er fulført/avbrut eller blir historisk
            .forEach {
                brukernotifikasjonService.setDone(it, VarselType.MOTE_SMS)
                moteSmsDAO.slettGjeldende(it)
            })
    }

    @PreDestroy
    @ExcludeFromCoverageGenerated
    fun stopScheduler() {
        scheduledExecutorService.shutdown()
    }
}
